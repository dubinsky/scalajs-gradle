package org.podval.tools.scalajs

import org.gradle.api.tasks.TaskExecutionException
import org.gradle.api.logging.Logger
import org.gradle.api.GradleException
import org.opentorah.build.Gradle.*
import org.opentorah.util.Files
import org.podval.tools.scalajs.dependencies.GradleUtil.*
import org.scalajs.jsenv.{Input, JSEnv, JSRun, RunConfig}
import org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv
import org.scalajs.linker.{PathIRContainer, PathOutputDirectory, StandardImpl}
import org.scalajs.linker.interface.{IRContainer, IRFile, LinkingException, ModuleInitializer, ModuleKind,
  ModuleSplitStyle, Report, Semantics, StandardConfig}
import sbt.io.IO
import org.scalajs.testing.adapter.TestAdapterInitializer
import java.io.{File, InputStream}
import java.nio.file.Path
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters.*

final class Actions(task: ScalaJSTask):
  private given CanEqual[ModuleKind, ModuleKind] = CanEqual.derived

  private def logger: Logger = task.getLogger
  private lazy val jsLogger: org.scalajs.logging.Logger = JSLogger(logger, task.getName)

  private val linkTask: LinkTask = task match
    case l: LinkTask => l
    case al: AfterLinkTask[?] => al.linkTask

  private val extension: Extension = linkTask.getProject.getExtension(classOf[Extension])
  private val moduleKind: ModuleKind = extension.getModuleKind.byName(ModuleKind.NoModule, ModuleKind.All)

  private def jsDirectory: File = linkTask.getJSDirectory
  private def reportTextFile: File = linkTask.getReportTextFile
  private def reportBinFile: File = linkTask.getReportBinFile

  private lazy val jsEnv: JSEnv = new JSDOMNodeJSEnv()

  private lazy val mainModule: Report.Module =
    val report: Report = Report
      .deserialize(IO.readBytes(reportBinFile))
      .get

    val result: Report.Module = report
      .publicModules
      .find(_.moduleID == "main")
      // TODO is running tests really conditional on the existence of the 'main' module?
      .getOrElse(throw GradleException(s"Linking result does not have a module named 'main'. Full report:\n$report"))

    require(moduleKind == result.moduleKind, s"moduleKind discrepancy: $moduleKind != ${result.moduleKind}")
    result

  private lazy val mainModulePath: Path = Files.file(directory = jsDirectory, segments = mainModule.jsFileName).toPath

  private def sourceMapper: SourceMapper = SourceMapper(
    sourceMapFile = mainModule.sourceMapName.map(name => Files.file(directory = jsDirectory, segments = name)),
    projectRootFile = linkTask.getProject.getRootDir
  )

  private def input: Input = moduleKind match
    case ModuleKind.NoModule       => Input.Script        (mainModulePath)
    case ModuleKind.ESModule       => Input.ESModule      (mainModulePath)
    case ModuleKind.CommonJSModule => Input.CommonJSModule(mainModulePath)

  def link(): Unit =
    val fullOptimization: Boolean = extension.stage == Stage.FullOpt

    val moduleInitializers: Seq[ModuleInitializer] = linkTask match
      case _: LinkTask.Main => extension
        .getModuleInitializers
        .asScala // TODO unify with toSet() used by the DocBook plugin
        .toSeq
        .map(Actions.toModuleInitializer)

      // Note: configured moduleInitializers are ignored for tests
      case _: LinkTask.Test => Seq(ModuleInitializer.mainMethod(
        TestAdapterInitializer.ModuleClassName,
        TestAdapterInitializer.MainMethodName
      ))

    val linkerConfig: StandardConfig = StandardConfig()
      .withCheckIR(fullOptimization)
      .withSemantics(if fullOptimization then Semantics.Defaults.optimized else Semantics.Defaults)
      .withModuleKind(moduleKind)
      .withClosureCompiler(fullOptimization && (moduleKind == ModuleKind.ESModule))
      .withModuleSplitStyle(extension.getModuleSplitStyle.byName(ModuleSplitStyle.FewestModules, Actions.moduleSplitStyles))
      .withPrettyPrint(extension.getPrettyPrint.getOrElse(false))

    logger.info(
      s"""ScalaJSPlugin:
         |JSDirectory = $jsDirectory
         |reportFile = $reportTextFile
         |moduleInitializers = ${moduleInitializers.map(ModuleInitializer.fingerprint).mkString(", ")}
         |linkerConfig = $linkerConfig
         |""".stripMargin,
      null, null, null)

    jsDirectory.mkdirs()

    try
      val report: Report = Await.result(atMost = Duration.Inf, awaitable = PathIRContainer
        .fromClasspath(linkTask.getRuntimeClassPath.getFiles.asScala.toSeq.map(_.toPath))
        .map(_._1)
        .flatMap((irContainers: Seq[IRContainer]) => StandardImpl.irFileCache.newCache.cached(irContainers))
        .flatMap((irFiles     : Seq[IRFile]     ) => StandardImpl.linker(linkerConfig).link(
          irFiles = irFiles,
          // TODO Without the initializers, no JavaScript is emitted - unless entry points are marked in what special way?
          moduleInitializers = moduleInitializers,
          output = PathOutputDirectory(jsDirectory.toPath),
          logger = jsLogger
        ))
      )

      Files.write(file = reportTextFile, content = report.toString())
      IO.write(reportBinFile, Report.serialize(report))
    catch
      case e: LinkingException => throw TaskExecutionException(linkTask, e)

  // TODO use SourceMapper to process the exceptions - if I can intercept them...
  def run(): Unit =
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

  // TODO use SourceMapper to process the exceptions
  def test(): Unit =
    // Note: scalaCompile.getAnalysisFiles is empty, so I had to hard-code the path:
    val scalaCompileAnalysisFile: File = Files.file(
      directory = linkTask.getProject.getBuildDir,
      segments  = s"tmp/scala/compilerAnalysis/${linkTask.classesTask.getScalaCompile.getName}.analysis"
    )

    Tests.run(
      jsEnv = jsEnv,
      input = input,
      analysisFile = scalaCompileAnalysisFile,
      jsLogger = jsLogger,
      listeners = TestListeners(sourceMapper, logger)
    )

object Actions:
  private val moduleSplitStyles: List[ModuleSplitStyle] = List(
    ModuleSplitStyle.FewestModules,
    ModuleSplitStyle.SmallestModules
  )

  private def toModuleInitializer(properties: ModuleInitializerProperties): ModuleInitializer =
    val clazz : String = properties.getClassName.get
    val method: String = properties.getMainMethodName.getOrElse("main")
    // TODO use the name as the module id:
    if properties.getMainMethodHasArgs.getOrElse(false)
    then ModuleInitializer.mainMethodWithArgs(clazz, method)//.withModuleID(getName)
    else ModuleInitializer.mainMethod        (clazz, method)//.withModuleID(getName)
