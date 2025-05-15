package org.podval.tools.scalajs

import org.podval.tools.util.Files
import org.scalajs.linker.interface.{IRContainer, IRFile, LinkingException, Report, Semantics, StandardConfig,
  ModuleInitializer as ModuleInitializerSJS, ModuleSplitStyle as ModuleSplitStyleSJS}
import org.scalajs.linker.{PathIRContainer, PathOutputDirectory, StandardImpl}
import org.scalajs.testing.adapter.TestAdapterInitializer
import org.slf4j.{Logger, LoggerFactory}
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import java.io.File

final class ScalaJSLink(common: ScalaJSCommon):
  def link(
    reportTextFile: File,
    optimization: Optimization,
    moduleSplitStyle: ModuleSplitStyle,
    moduleInitializers: Option[Seq[ModuleInitializer]],
    prettyPrint: Boolean,
    runtimeClassPath: Seq[File],
  ): Unit =
    // Tests use fixed entry point.
    val moduleInitializersSJS: Seq[ModuleInitializerSJS] = moduleInitializers
      .map(_.map(ScalaJSLink.toSJS))
      .getOrElse(Seq(ModuleInitializerSJS.mainMethod(
        TestAdapterInitializer.ModuleClassName,
        TestAdapterInitializer.MainMethodName
      )))

    val fullOptimization: Boolean = optimization == Optimization.Full
    val linkerConfig: StandardConfig = StandardConfig()
      .withCheckIR(fullOptimization)
      .withSemantics(if fullOptimization then Semantics.Defaults.optimized else Semantics.Defaults)
      .withModuleKind(common.moduleKindSJS)
      .withClosureCompiler(fullOptimization && (common.moduleKind == ModuleKind.ESModule))
      .withModuleSplitStyle(ScalaJSLink.toSJS(moduleSplitStyle))
      // TODO .withESFeatures(org.scalajs.linker.interface.ESFeatures)
      // TODO .withExperimentalUseWebAssembly(Boolean)
      .withPrettyPrint(prettyPrint)

    ScalaJSLink.logger.info(
      s"""${common.logSource}:
         |JSDirectory = ${common.jsDirectory}
         |reportFile = $reportTextFile
         |moduleInitializers = ${moduleInitializersSJS.map(ModuleInitializerSJS.fingerprint).mkString(", ")}
         |linkerConfig = $linkerConfig
         |""".stripMargin
    )

    common.jsDirectory.mkdirs()

    try
      val report: Report = Await.result(atMost = Duration.Inf, awaitable = PathIRContainer
        .fromClasspath(runtimeClassPath.map(_.toPath))
        .map(_._1)
        .flatMap((irContainers: Seq[IRContainer]) => StandardImpl.irFileCache.newCache.cached(irContainers))
        .flatMap((irFiles: Seq[IRFile]) => StandardImpl.linker(linkerConfig).link(
          irFiles = irFiles,
          moduleInitializers = moduleInitializersSJS,
          output = PathOutputDirectory(common.jsDirectory.toPath),
          logger = common.loggerJS
        ))
      )

      Files.write(reportTextFile, report.toString)
      Files.writeBytes(common.reportBinFile, Report.serialize(report))
    catch
      case e: LinkingException => throw common.abort(s"ScalaJS link error: ${e.getMessage}")

object ScalaJSLink:
  private val logger: Logger = LoggerFactory.getLogger(ScalaJSLink.getClass)
  
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
