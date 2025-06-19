package org.podval.tools.scalajs

import org.podval.tools.util.Files
import org.scalajs.jsenv.Input
import org.scalajs.linker.{PathIRContainer, PathOutputDirectory, StandardImpl}
import org.scalajs.linker.interface.{IRContainer, IRFile, LinkingException, Report, Semantics, StandardConfig,
  ModuleInitializer as ModuleInitializerSJS, ModuleKind as ModuleKindSJS, ModuleSplitStyle as ModuleSplitStyleSJS}
import org.scalajs.testing.adapter.TestAdapterInitializer
import scala.concurrent.Await
import java.io.File
import java.nio.file.Path
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

final class ScalaJSLink(
  val jsDirectory: File,
  reportBinFile: File,
  moduleKind: ModuleKind,
  val useWebAssembly: Boolean,
  logSource: String
) extends ScalaJSBuild(
  logSource
):
  def module(jsEnvKind: JSEnvKind): (Report.Module, Path, Input) =
    if jsEnvKind == JSEnvKind.JSDOMNodeJS && moduleKind != ModuleKind.NoModule then
      abort(s"`jsEnv = 'Node.js+DOM' requires `moduleKind = 'NoModule'`")
    if jsEnvKind == JSEnvKind.Playwright && moduleKind != ModuleKind.ESModule then
      abort(s"`jsEnv = 'Playwright'` requires `moduleKind = 'ESModule'`; see https://github.com/gmkumar2005/scala-js-env-playwright")

    val module: Report.Module = Report
      .deserialize(Files.readBytes(reportBinFile))
      .get
      .publicModules
      .find(_.moduleID == "main")
      .getOrElse(abort(s"Linking report does not have a module named 'main'. See $reportBinFile."))

    given CanEqual[ModuleKindSJS, ModuleKindSJS] = CanEqual.derived
    require(
      moduleKindSJS == module.moduleKind,
      s"moduleKind discrepancy: $moduleKind != ${module.moduleKind}"
    )

    val path: Path = Files.file(
      directory = jsDirectory,
      segments = module.jsFileName
    ).toPath

    val path2input: Path => Input = moduleKind match
      case ModuleKind.NoModule       => Input.Script
      case ModuleKind.ESModule       => Input.ESModule
      case ModuleKind.CommonJSModule => Input.CommonJSModule

    (module, path, path2input(path))

  private def moduleKindSJS: ModuleKindSJS = moduleKind match
    case ModuleKind.NoModule => ModuleKindSJS.NoModule
    case ModuleKind.ESModule => ModuleKindSJS.ESModule
    case ModuleKind.CommonJSModule => ModuleKindSJS.CommonJSModule

  def link(
    reportTextFile: File,
    runtimeClassPath: Seq[File],
    optimization: Optimization,
    moduleSplitStyle: ModuleSplitStyle,
    smallModulesFor: List[String],
    moduleInitializers: Option[Seq[ModuleInitializer]],
    prettyPrint: Boolean
  ): Unit =
    validateLink(moduleSplitStyle)

    val fullOptimization: Boolean = optimization == Optimization.Full

    val moduleSplitStyleSJS: ModuleSplitStyleSJS = moduleSplitStyle match
      case ModuleSplitStyle.FewestModules => ModuleSplitStyleSJS.FewestModules
      case ModuleSplitStyle.SmallestModules => ModuleSplitStyleSJS.SmallestModules
      case ModuleSplitStyle.SmallModulesFor => ModuleSplitStyleSJS.SmallModulesFor(smallModulesFor)

    val linkerConfig: StandardConfig = StandardConfig()
      .withCheckIR(fullOptimization)
      .withSemantics(if fullOptimization then Semantics.Defaults.optimized else Semantics.Defaults)
      .withModuleKind(moduleKindSJS)
      .withClosureCompiler(fullOptimization && (moduleKind == ModuleKind.ESModule))
      .withModuleSplitStyle(moduleSplitStyleSJS)
      .withExperimentalUseWebAssembly(useWebAssembly)
      .withPrettyPrint(prettyPrint)

    // Tests use fixed entry point.
    val moduleInitializersSJS: Seq[ModuleInitializerSJS] = moduleInitializers
      .map(_.map(ScalaJSLink.toSJS))
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
          logger = loggerJS
        ))
      )

      Files.write(reportTextFile, report.toString)
      Files.writeBytes(reportBinFile, Report.serialize(report))
    catch
      case e: LinkingException => abort(s"ScalaJS link error: ${e.getMessage}")

  private def validateLink(moduleSplitStyle: ModuleSplitStyle): Unit =
    if useWebAssembly && moduleKind != ModuleKind.ESModule then
      abort(s"`experimentalUseWebAssembly = true` requires `moduleKind = 'ESModule'`; see https://www.scala-js.org/doc/project/webassembly.html")

    if useWebAssembly && moduleSplitStyle != ModuleSplitStyle.FewestModules then
      abort(s"`experimentalUseWebAssembly = true` requires `moduleSplitStyle = 'FewestModules'`; see https://www.scala-js.org/doc/project/webassembly.html")

    if moduleKind == ModuleKind.NoModule && moduleSplitStyle != ModuleSplitStyle.FewestModules then
      abort(s"moduleKind = 'NoModule'` requires `moduleSplitStyle = 'FewestModules'`")

object ScalaJSLink:
  private def toSJS(moduleInitializer: ModuleInitializer): ModuleInitializerSJS =
    val clazz: String = moduleInitializer.className
    val method: String = moduleInitializer.mainMethodName.getOrElse("main")
    val result: ModuleInitializerSJS =
      if moduleInitializer.mainMethodHasArgs
      then ModuleInitializerSJS.mainMethodWithArgs(clazz, method)
      else ModuleInitializerSJS.mainMethod(clazz, method)
    result.withModuleID(moduleInitializer.moduleId)
