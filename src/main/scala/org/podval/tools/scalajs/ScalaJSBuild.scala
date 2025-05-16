package org.podval.tools.scalajs

import org.podval.tools.node.Node
import org.podval.tools.nonjvm.NonJvmTestAdapter
import org.podval.tools.util.Files
import org.scalajs.jsenv.{Input, JSEnv, JSRun, RunConfig}
import org.scalajs.linker.{PathIRContainer, PathOutputDirectory, StandardImpl}
import org.scalajs.linker.interface.{IRContainer, IRFile, LinkingException, Report, Semantics, StandardConfig,
  ModuleInitializer as ModuleInitializerSJS, ModuleKind as ModuleKindSJS, ModuleSplitStyle as ModuleSplitStyleSJS}
import org.scalajs.logging.{Level as LevelJS, Logger as LoggerJS}
import org.scalajs.testing.adapter.{TestAdapter, TestAdapterInitializer}
import org.slf4j.{Logger, LoggerFactory}
import sbt.testing.Framework
import java.io.File
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object ScalaJSBuild:
  private val logger: Logger = LoggerFactory.getLogger(ScalaJSBuild.getClass)

  // TODO add logSource in Scala Native
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

  def run(
    jsEnv: JSEnv,
    input: Input,
    config: RunConfig,
    logSource: String
  ): Unit =
    val jsRun: JSRun = jsEnv.start(
      Seq(input),
      config
        .withLogger(loggerJS(logSource))
    )
    Await.result(awaitable = jsRun.future, atMost = Duration.Inf)

  def createTestEnvironment(
    jsDirectory: File,
    jsEnv: JSEnv,
    mainModule: Report.Module,
    input: Input,
    logSource: String
  ): ScalaJSTestEnvironment = ScalaJSTestEnvironment(
    sourceMapper = mainModule
      .sourceMapName
      .map((name: String) => Files.file(jsDirectory, name))
      .map(ClosureCompilerSourceMapper(_)),
    testAdapter = new NonJvmTestAdapter:
      override def loadFrameworks(frameworkNames: List[List[String]]): List[Option[Framework]] =
        testAdapter.loadFrameworks(frameworkNames)
      override def close(): Unit = testAdapter.close()
      private lazy val testAdapter: TestAdapter = TestAdapter(
        jsEnv = jsEnv,
        input = Seq(input),
        config = TestAdapter.Config().withLogger(loggerJS(logSource))
      )
  )

  def link(
    jsDirectory: File,
    reportTextFile: File,
    reportBinFile: File,
    runtimeClassPath: Seq[File],
    optimization: Optimization,
    moduleKind: ModuleKind,
    moduleSplitStyle: ModuleSplitStyle,
    moduleInitializers: Option[Seq[ModuleInitializer]],
    prettyPrint: Boolean,
    logSource: String,
    abort: String => Exception
  ): Unit =
    val fullOptimization: Boolean = optimization == Optimization.Full
    val moduleKindSJS: ModuleKindSJS = toSJS(moduleKind)
    val linkerConfig: StandardConfig = StandardConfig()
      .withCheckIR(fullOptimization)
      .withSemantics(if fullOptimization then Semantics.Defaults.optimized else Semantics.Defaults)
      .withModuleKind(moduleKindSJS)
      .withClosureCompiler(fullOptimization && (moduleKind == ModuleKind.ESModule))
      .withModuleSplitStyle(toSJS(moduleSplitStyle))
      // TODO .withESFeatures(org.scalajs.linker.interface.ESFeatures)
      // TODO .withExperimentalUseWebAssembly(Boolean)
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

  def toSJS(moduleKind: ModuleKind): ModuleKindSJS = moduleKind match
    case ModuleKind.NoModule       => ModuleKindSJS.NoModule
    case ModuleKind.ESModule       => ModuleKindSJS.ESModule
    case ModuleKind.CommonJSModule => ModuleKindSJS.CommonJSModule

  private def toSJS(moduleSplitStyle: ModuleSplitStyle): ModuleSplitStyleSJS = moduleSplitStyle match
    case ModuleSplitStyle.FewestModules => ModuleSplitStyleSJS.FewestModules
    case ModuleSplitStyle.SmallestModules => ModuleSplitStyleSJS.SmallestModules

  private def toSJS(moduleInitializer: ModuleInitializer): ModuleInitializerSJS =
    val clazz: String = moduleInitializer.className
    val method: String = moduleInitializer.mainMethodName.getOrElse("main")
    val result: ModuleInitializerSJS =
      if moduleInitializer.mainMethodHasArgs
      then ModuleInitializerSJS.mainMethodWithArgs(clazz, method)
      else ModuleInitializerSJS.mainMethod(clazz, method)
    result.withModuleID(moduleInitializer.moduleId)
