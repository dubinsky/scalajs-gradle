package org.podval.tools.scalajs

// TODO disentangle from Gradle
import org.gradle.api.GradleException
import org.gradle.api.logging.{Logger, LogLevel as GLevel}
import org.podval.tools.files.PipeOutputThread
import org.podval.tools.testing.framework.FrameworkDescriptor
import org.podval.tools.testing.task.{SourceMapper, TestEnvironment}
import org.podval.tools.util.Files
import org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv
import org.scalajs.jsenv.{Input, JSEnv, JSRun, RunConfig}
import org.scalajs.linker.interface.{IRContainer, IRFile, LinkingException, ModuleInitializer as ModuleInitializerSJS,
  ModuleKind as ModuleKindSJS, ModuleSplitStyle as ModuleSplitStyleSJS, Report, Semantics, StandardConfig}
import org.scalajs.linker.{PathIRContainer, PathOutputDirectory, StandardImpl}
import org.scalajs.logging.Level as JSLevel
import org.scalajs.testing.adapter.{TestAdapter, TestAdapterInitializer}
import sbt.testing.Framework
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import java.io.{File, InputStream}
import java.nio.file.Path

final class ScalaJS(
  nodePath: String, // TODO not needed for link()
  nodeEnvironment: Map[String, String], // TODO not needed for link()
  jsDirectory: File,
  reportBinFile: File,
  reportTextFile: File,
  optimization: Optimization,
  moduleKind: ModuleKind,
  moduleSplitStyle: ModuleSplitStyle,
  moduleInitializers: Option[Seq[ModuleInitializer]],
  prettyPrint: Boolean,
  runtimeClassPath: Seq[File],
  logger: Logger,
  logSource: String
):
  private def jsLogger: org.scalajs.logging.Logger = new org.scalajs.logging.Logger:
    override def trace(t: => Throwable): Unit =
      logger.error(s"$logSource Error", t)
    override def log(level: JSLevel, message: => String): Unit =
      logger.log(ScalaJS.scalajs2gradleLevel(level), s"$logSource: $message")

  def link(): Unit =
    // Note: tests use fixed entry point
    val moduleInitializersSJS: Seq[ModuleInitializerSJS] = moduleInitializers
      .map(_.map(ScalaJS.toModuleInitializer))
      .getOrElse(Seq(ScalaJS.testAdapterInitializer))

    val fullOptimization: Boolean = optimization == Optimization.Full
    val linkerConfig: StandardConfig = StandardConfig()
      .withCheckIR(fullOptimization)
      .withSemantics(if fullOptimization then Semantics.Defaults.optimized else Semantics.Defaults)
      .withModuleKind(ScalaJS.toJs(moduleKind))
      .withClosureCompiler(fullOptimization && (moduleKind == ModuleKind.ESModule))
      .withModuleSplitStyle(moduleSplitStyle match {
        case ModuleSplitStyle.FewestModules => ModuleSplitStyleSJS.FewestModules
        case ModuleSplitStyle.SmallestModules => ModuleSplitStyleSJS.SmallestModules
      })
      .withPrettyPrint(prettyPrint)

    logger.info(
      s"""$logSource:
         |JSDirectory = $jsDirectory
         |reportFile = $reportTextFile
         |moduleInitializers = ${moduleInitializersSJS.map(ModuleInitializerSJS.fingerprint).mkString(", ")}
         |linkerConfig = $linkerConfig
         |""".stripMargin,
      null, null, null)

    jsDirectory.mkdirs()

    try
      val report: Report = Await.result(atMost = Duration.Inf, awaitable = PathIRContainer
        .fromClasspath(runtimeClassPath.map(_.toPath))
        .map(_._1)
        .flatMap((irContainers: Seq[IRContainer]) => StandardImpl.irFileCache.newCache.cached(irContainers))
        .flatMap((irFiles: Seq[IRFile]) => StandardImpl.linker(linkerConfig).link(
          irFiles = irFiles,
          moduleInitializers = moduleInitializersSJS,
          output = PathOutputDirectory(jsDirectory.toPath),
          logger = jsLogger
        ))
      )

      Files.write(reportTextFile, report.toString)
      Files.writeBytes(reportBinFile, Report.serialize(report))
    catch
      case e: LinkingException => throw GradleException("ScalaJS link error", e)

  private lazy val mainModule: Report.Module =
    val result: Report.Module = Report
      .deserialize(Files.readBytes(reportBinFile))
      .get
      .publicModules
      .find(_.moduleID == "main")
      .getOrElse(throw GradleException(s"Linking report does not have a module named 'main'. See $reportBinFile"))

    given CanEqual[ModuleKindSJS, ModuleKindSJS] = CanEqual.derived
    require(ScalaJS.toJs(moduleKind) == result.moduleKind, s"moduleKind discrepancy: $moduleKind != ${result.moduleKind}")
    result

  private def mainModulePath: Path = Files.file(
    directory = jsDirectory,
    segments = mainModule.jsFileName
  ).toPath

  private def input: Input = moduleKind match
    case ModuleKind.NoModule       => Input.Script        (mainModulePath)
    case ModuleKind.ESModule       => Input.ESModule      (mainModulePath)
    case ModuleKind.CommonJSModule => Input.CommonJSModule(mainModulePath)

  private def mkJsEnv: JSEnv =
    JSDOMNodeJSEnv(JSDOMNodeJSEnv.Config()
      .withExecutable(nodePath)
      .withEnv(nodeEnvironment)
    )

  def run(): Unit =
    val jsEnv: JSEnv = mkJsEnv
    logger.lifecycle(s"Running $mainModulePath on ${jsEnv.name}\n")

    /* The list of threads that are piping output to System.out and
     * System.err. This is not an AtomicReference or any other thread-safe
     * structure because:
     * - `onOutputStream` is guaranteed to be called exactly once, and
     * - `pipeOutputThreads` is only read once the run is completed
     *   (although the JSEnv interface does not explicitly specify that the
     *   call to `onOutputStream must happen before that, anything else is
     *   just plain unreasonable).
     * We only mark it as `@volatile` to ensure that there is an
     * appropriate memory barrier between writing to it and reading it back.
     */
    @volatile var pipeOutputThreads: List[Thread] = Nil

    /* #4560 Explicitly redirect out/err to System.out/System.err, instead
     * of relying on `inheritOut` and `inheritErr`, so that streams
     * installed with `System.setOut` and `System.setErr` are always taken
     * into account. sbt installs such alternative outputs when it runs in
     * server mode.
     */
    val config: RunConfig = RunConfig()
      .withLogger(jsLogger)
      .withInheritOut(false)
      .withInheritErr(false)
      .withOnOutputStream((out: Option[InputStream], err: Option[InputStream]) => pipeOutputThreads =
        PipeOutputThread.pipe(out, logger.lifecycle, "out: ") :::
        PipeOutputThread.pipe(err, logger.error    , "err: ")
      )

    // TODO Without this delay (or with a shorter one)
    // above log message (and task name logging that comes before it)
    // appear AFTER the output of the run; there should be a better way
    // to ensure that Gradle logging comes first...
    Thread.sleep(2000)

    try
      val run: JSRun = jsEnv.start(Seq(input), config)
      Await.result(awaitable = run.future, atMost = Duration.Inf)
    finally
    /* Wait for the pipe output threads to be done, to make sure that we
     * do not finish the `run` task before *all* output has been
     * transferred to System.out and System.err.
     * We do that in a `finally` block so that the stdout and stderr
     * streams are propagated even if the run finishes with a failure.
     * `join()` itself does not throw except if the current thread is
     * interrupted, which is not supposed to happen (if it does happen,
     * the interrupted exception will shadow any error from the run).
     */
      pipeOutputThreads.foreach(_.join)

  def sourceMapper: Option[SourceMapper] = mainModule
    .sourceMapName
    .map((name: String) => Files.file(jsDirectory, name))
    .map(ClosureCompilerSourceMapper(_))

  def testEnvironment: TestEnvironment = new TestEnvironment:
    private val testAdapter: TestAdapter = TestAdapter(
      jsEnv = mkJsEnv,
      input = Seq(input),
      config = TestAdapter.Config().withLogger(jsLogger)
    )

    override def loadFrameworks(testClassPath: Iterable[File]): List[Framework] = testAdapter
      .loadFrameworks(FrameworkDescriptor
        .scalaJSSupported
        .map((descriptor: FrameworkDescriptor) => List(descriptor.className))
      )
      .flatten

    override def close(): Unit =
      testAdapter.close()

object ScalaJS:
  private def toModuleInitializer(moduleInitializer: ModuleInitializer): ModuleInitializerSJS =
    val clazz: String = moduleInitializer.className
    val method: String = moduleInitializer.mainMethodName.getOrElse("main")
    val result: ModuleInitializerSJS =
      if moduleInitializer.mainMethodHasArgs
      then ModuleInitializerSJS.mainMethodWithArgs(clazz, method)
      else ModuleInitializerSJS.mainMethod(clazz, method)
    result.withModuleID(moduleInitializer.moduleId)

  private def testAdapterInitializer: ModuleInitializerSJS = ModuleInitializerSJS.mainMethod(
    TestAdapterInitializer.ModuleClassName,
    TestAdapterInitializer.MainMethodName
  )

  private def toJs(moduleKind: ModuleKind): ModuleKindSJS = moduleKind match {
    case ModuleKind.NoModule => ModuleKindSJS.NoModule
    case ModuleKind.ESModule => ModuleKindSJS.ESModule
    case ModuleKind.CommonJSModule => ModuleKindSJS.CommonJSModule
  }

  private given CanEqual[JSLevel, JSLevel] = CanEqual.derived
  private def scalajs2gradleLevel(level: JSLevel): GLevel = level match
    case JSLevel.Error => GLevel.ERROR
    case JSLevel.Warn  => GLevel.WARN
    case JSLevel.Info  => GLevel.INFO
    case JSLevel.Debug => GLevel.DEBUG
