package org.podval.tools.backend.scalajs

//import jsenv.playwright.PWEnv
import org.podval.tools.backend.nonjvm.NonJvmTestAdapter
import org.podval.tools.node.Node
import org.podval.tools.platform.{OutputHandler, OutputPiper}
import org.podval.tools.util.Files
import org.scalajs.jsenv.{Input, JSEnv, JSRun, RunConfig}
import org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv
import org.scalajs.jsenv.nodejs.NodeJSEnv
import org.scalajs.linker.{PathIRContainer, PathOutputDirectory, StandardImpl}
import org.scalajs.linker.interface.{IRContainer, IRFile, LinkingException, Report, Semantics, StandardConfig,
  ModuleInitializer as ModuleInitializerSJS, ModuleKind as ModuleKindSJS, ModuleSplitStyle as ModuleSplitStyleSJS}
import org.scalajs.logging.{Level as LevelJS, Logger as LoggerJS}
import org.scalajs.testing.adapter.{TestAdapter, TestAdapterInitializer}
import org.slf4j.{Logger, LoggerFactory}
import sbt.testing.Framework
import java.io.{File, InputStream}
import java.nio.file.Path
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object ScalaJSBuild:
  private val logger: Logger = LoggerFactory.getLogger(ScalaJSBuild.getClass)

  private def loggerJS(logSource: String): LoggerJS = new LoggerJS:
    override def trace(t: => Throwable): Unit =
      logger.error(s"$logSource Error", t)

    override def log(level: LevelJS, message: => String): Unit =
      val toLog: String = s"$logSource: $message"

      // Gradle has its own copy of org.slf4j API, which predates introduction of logger.atLevel().
      given CanEqual[LevelJS, LevelJS] = CanEqual.derived
      level match
        case LevelJS.Error => logger.error(toLog)
        case LevelJS.Warn  => logger.warn (toLog)
        case LevelJS.Info  => logger.info (toLog)
        case LevelJS.Debug => logger.debug(toLog)

  def link(
    jsDirectory: File,
    reportTextFile: File,
    reportBinFile: File,
    runtimeClassPath: Seq[File],
    optimization: Optimization,
    moduleKind: ModuleKind,
    moduleSplitStyle: ModuleSplitStyle,
    useWebAssembly: Boolean,
    moduleInitializers: Option[Seq[ModuleInitializer]],
    prettyPrint: Boolean,
    logSource: String,
    abort: String => Exception
  ): Unit =
    val fullOptimization: Boolean = optimization == Optimization.Full
    val moduleKindEffective: ModuleKind = adjustModuleKind(moduleKind, useWebAssembly)
    val moduleSplitStyleEffective: ModuleSplitStyle = adjustModuleSplitStyle(moduleSplitStyle, useWebAssembly)

    val linkerConfig: StandardConfig = StandardConfig()
      .withCheckIR(fullOptimization)
      .withSemantics(if fullOptimization then Semantics.Defaults.optimized else Semantics.Defaults)
      .withModuleKind(toSJS(moduleKindEffective))
      .withClosureCompiler(fullOptimization && (moduleKindEffective == ModuleKind.ESModule))
      .withModuleSplitStyle(toSJS(moduleSplitStyleEffective))
      .withExperimentalUseWebAssembly(useWebAssembly)
      .withPrettyPrint(prettyPrint)

    // Tests use fixed entry point.
    val moduleInitializersSJS: Seq[ModuleInitializerSJS] = moduleInitializers
      .map(_.map(toSJS))
      .getOrElse(Seq(ModuleInitializerSJS.mainMethod(
        TestAdapterInitializer.ModuleClassName,
        TestAdapterInitializer.MainMethodName
      )))

    logger.info(
      s"""$logSource:
         |JSDirectory = $jsDirectory
         |reportFile = $reportTextFile
         |moduleInitializers = ${moduleInitializersSJS.map(ModuleInitializerSJS.fingerprint).mkString(", ")}
         |linkerConfig = $linkerConfig
         |""".stripMargin
    )

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
          logger = loggerJS(logSource)
        ))
      )

      Files.write(reportTextFile, report.toString)
      Files.writeBytes(reportBinFile, Report.serialize(report))
    catch
      case e: LinkingException => throw abort(s"ScalaJS link error: ${e.getMessage}")

  def run(
    jsDirectory: File,
    reportBinFile: File,
    moduleKind: ModuleKind,
    node: Node,
    useWebAssembly: Boolean,
    jsEnvKind: JSEnvKind,
    browserName: BrowserName,
    logSource: String,
    abort: String => Nothing,
    outputHandler: OutputHandler
  ): Unit =
    val moduleKindEffective: ModuleKind = adjustModuleKind(adjustModuleKind(moduleKind, useWebAssembly), jsEnvKind)
    val module: Report.Module = ScalaJSBuild.module(reportBinFile, moduleKindEffective, abort)

    OutputPiper.run(
      outputHandler,
      running = s"${modulePath(jsDirectory, module)}"
    ): (outputPiper: OutputPiper) =>
    /* #4560 Explicitly redirect out/err to System.out/System.err, instead
     * of relying on `inheritOut` and `inheritErr`, so that streams
     * installed with `System.setOut` and `System.setErr` are always taken
     * into account. sbt installs such alternative outputs when it runs in
     * server mode.
     */
      val jsRun: JSRun = mkJsEnv(
        node,
        adjustJSEnvKind(jsEnvKind, moduleKindEffective),
        useWebAssembly,
        browserName,
        logSource
      ).start(
        Seq(input(jsDirectory, module, moduleKindEffective)),
        RunConfig()
          .withInheritOut(false)
          .withInheritErr(false)
          .withOnOutputStream((out: Option[InputStream], err: Option[InputStream]) => outputPiper.start(out, err))
          .withLogger(loggerJS(logSource))
      )
      Await.result(awaitable = jsRun.future, atMost = Duration.Inf)

  def createTestEnvironment(
    jsDirectory: File,
    reportBinFile: File,
    moduleKind: ModuleKind,
    node: Node,
    useWebAssembly: Boolean,
    jsEnvKind: JSEnvKind,
    browserName: BrowserName,
    logSource: String,
    abort: String => Nothing
  ): ScalaJSTestEnvironment =
    val moduleKindEffective: ModuleKind = adjustModuleKind(adjustModuleKind(moduleKind, useWebAssembly), jsEnvKind)
    val module: Report.Module = ScalaJSBuild.module(reportBinFile, moduleKindEffective, abort)

    ScalaJSTestEnvironment(
      sourceMapper = module
        .sourceMapName
        .map((name: String) => Files.file(jsDirectory, name))
        .map(ClosureCompilerSourceMapper(_)),
      testAdapter = new NonJvmTestAdapter:
        override def loadFrameworks(frameworkNames: List[List[String]]): List[Option[Framework]] =
          testAdapter.loadFrameworks(frameworkNames)
        override def close(): Unit = testAdapter.close()
        private lazy val testAdapter: TestAdapter = TestAdapter(
          jsEnv = mkJsEnv(
            node,
            adjustJSEnvKind(jsEnvKind, moduleKindEffective),
            useWebAssembly,
            browserName,
            logSource
          ),
          input = Seq(input(jsDirectory, module, moduleKindEffective)),
          config = TestAdapter.Config().withLogger(loggerJS(logSource))
        )
    )

  private def adjustModuleKind(
    moduleKind: ModuleKind,
    useWebAssembly: Boolean
  ): ModuleKind =
    if useWebAssembly && moduleKind != ModuleKind.ESModule then
      logger.warn(s"ModuleKind set to ESModule because useWebAssembly requires it; see https://www.scala-js.org/doc/project/webassembly.html")
      ModuleKind.ESModule
    else moduleKind

  private def adjustModuleKind(
    moduleKind: ModuleKind,
    jsEnvKind: JSEnvKind
  ): ModuleKind =
    moduleKind
//    if jsEnvKind == JSEnvKind.Playwright && moduleKind != ModuleKind.ESModule then
//      logger.warn(s"ModuleKind set to ESModule because Playwright JavaScript environment requires it; see https://github.com/gmkumar2005/scala-js-env-playwright")
//      ModuleKind.ESModule
//    else moduleKind

  private def adjustModuleSplitStyle(
    moduleSplitStyle: ModuleSplitStyle,
    useWebAssembly: Boolean
  ): ModuleSplitStyle =
    if useWebAssembly && moduleSplitStyle != ModuleSplitStyle.FewestModules then
      logger.warn(s"ModuleSplitStyle set to FewestModules because useWebAssembly requires it; see https://www.scala-js.org/doc/project/webassembly.html")
      ModuleSplitStyle.FewestModules
    else moduleSplitStyle

  private def adjustJSEnvKind(
    jsEnvKind: JSEnvKind,
    moduleKind: ModuleKind
  ): JSEnvKind =
    if jsEnvKind == JSEnvKind.JSDOMNodeJS && moduleKind != ModuleKind.NoModule then
      logger.warn(s"JSEnv set to NodeJSEnv because JSDOMNodeJSEnv does not support ModuleKind $moduleKind")
      JSEnvKind.NodeJS
    else jsEnvKind

  private def mkJsEnv(
    node: Node,
    jsEnvKind: JSEnvKind,
    useWebAssembly: Boolean,
    browserName: BrowserName,
    logSource: String
  ): JSEnv =
    val executable: String = node.installation.node.getAbsolutePath
    val env: Map[String, String] = node.nodeEnv.toMap

    // see https://www.scala-js.org/doc/project/webassembly.html
    def args: List[String] = if !useWebAssembly then List.empty else List(
      "--experimental-wasm-exnref", // always required
      "--experimental-wasm-jspi", // required for js.async/js.await
      "--experimental-wasm-imported-strings", // optional (good for performance)
    )

    jsEnvKind match
      case JSEnvKind.NodeJS =>
        val config: NodeJSEnv.Config = NodeJSEnv.Config()
          .withExecutable(executable)
          .withEnv(env)
          .withArgs(args)
        logger.info(s"$logSource: jsEnv=NodeJSEnv($config)")
        NodeJSEnv(config)

      case JSEnvKind.JSDOMNodeJS =>
        val config: JSDOMNodeJSEnv.Config = JSDOMNodeJSEnv.Config()
          .withExecutable(executable)
          .withEnv(env)
          .withArgs(args)
        logger.info(s"$logSource: jsEnv=JSDOMNodeJSEnv($config)")
        JSDOMNodeJSEnv(config)

//      case JSEnvKind.Playwright =>
//        val config: PWEnv.Config = PWEnv.Config()
//        logger.info(s"$logSource: jsEnv=PWEnv($config), browserName=$browserName")
//        PWEnv(
//          browserName = browserName.name,
////          headless = ???,
////          showLogs = ???,
////          debug = ???,
//          pwConfig = config
//        )

  private def module(
    reportBinFile: File,
    moduleKind: ModuleKind,
    abort: String => Nothing
  ): Report.Module =
    val result: Report.Module = Report
      .deserialize(Files.readBytes(reportBinFile))
      .get
      .publicModules
      .find(_.moduleID == "main")
      .getOrElse(abort(s"Linking report does not have a module named 'main'. See $reportBinFile."))

    given CanEqual[ModuleKindSJS, ModuleKindSJS] = CanEqual.derived

    require(
      ScalaJSBuild.toSJS(moduleKind) == result.moduleKind,
      s"moduleKind discrepancy: $moduleKind != ${result.moduleKind}"
    )
    result

  private def modulePath(
    jsDirectory: File,
    module: Report.Module
  ): Path = Files.file(
    directory = jsDirectory,
    segments = module.jsFileName
  ).toPath

  private def input(
    jsDirectory: File,
    module: Report.Module,
    moduleKind: ModuleKind
  ): Input =
    val modulePath: Path = ScalaJSBuild.modulePath(jsDirectory, module)
    moduleKind match
      case ModuleKind.NoModule       => Input.Script        (modulePath)
      case ModuleKind.ESModule       => Input.ESModule      (modulePath)
      case ModuleKind.CommonJSModule => Input.CommonJSModule(modulePath)

  private def toSJS(moduleKind: ModuleKind): ModuleKindSJS = moduleKind match
    case ModuleKind.NoModule       => ModuleKindSJS.NoModule
    case ModuleKind.ESModule       => ModuleKindSJS.ESModule
    case ModuleKind.CommonJSModule => ModuleKindSJS.CommonJSModule

  private def toSJS(moduleSplitStyle: ModuleSplitStyle): ModuleSplitStyleSJS = moduleSplitStyle match
    case ModuleSplitStyle.FewestModules   => ModuleSplitStyleSJS.FewestModules
    case ModuleSplitStyle.SmallestModules => ModuleSplitStyleSJS.SmallestModules

  private def toSJS(moduleInitializer: ModuleInitializer): ModuleInitializerSJS =
    val clazz: String = moduleInitializer.className
    val method: String = moduleInitializer.mainMethodName.getOrElse("main")
    val result: ModuleInitializerSJS =
      if moduleInitializer.mainMethodHasArgs
      then ModuleInitializerSJS.mainMethodWithArgs(clazz, method)
      else ModuleInitializerSJS.mainMethod(clazz, method)
    result.withModuleID(moduleInitializer.moduleId)
