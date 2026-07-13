package org.podval.tools.scalajs

import org.podval.tools.build.Output
import org.podval.tools.nonjvm.Link
import org.podval.tools.util.Files
import org.scalajs.jsenv.Input
import org.scalajs.linker.{PathIRContainer, PathOutputDirectory, StandardImpl}
import org.scalajs.linker.interface.{IRContainer, IRFile, LinkingException, Report, Semantics, StandardConfig,
  ESVersion as ESVersionSJS, ModuleInitializer as ModuleInitializerSJS, ModuleKind as ModuleKindSJS, ModuleSplitStyle as ModuleSplitStyleSJS}
import org.scalajs.testing.adapter.TestAdapterInitializer
import scala.concurrent.Await
import java.io.File
import java.nio.file.Path
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

final class ScalaJSLink(
  val jsDirectory: File,
  reportBinFile: File,
  reportTextFile: File,
  moduleKind: ModuleKind,
  val useWebAssembly: Boolean,
  useJSPI: Boolean,
  runtimeClasspath: Seq[File],
  optimization: Optimization,
  moduleSplitStyle: ModuleSplitStyle,
  esVersion: ESVersion,
  smallModulesFor: List[String],
  moduleInitializers: Option[Seq[ModuleInitializer]],
  isTest: Boolean,
  prettyPrint: Boolean,
  output: Output
) extends ScalaJSBuild(output) with Link[ScalaJSBackend.type]:
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

  override def link(): Unit =
    // TODO move into constructor?

    if moduleKind == ModuleKind.NoModule && moduleSplitStyle != ModuleSplitStyle.FewestModules then
      abort(s"`moduleKind = 'NoModule'` requires `moduleSplitStyle = 'FewestModules'`")

    if useWebAssembly then
      if moduleSplitStyle != ModuleSplitStyle.FewestModules then
        abort(s"`useWebAssembly = true` requires `moduleSplitStyle = 'FewestModules'`; see https://www.scala-js.org/doc/project/webassembly.html")

      if moduleKind != ModuleKind.ESModule then
        abort(s"`useWebAssembly = true` requires `moduleKind = 'ESModule'`; see https://www.scala-js.org/doc/project/webassembly.html")

      if esVersion.year < 2022 then
        abort(s"`useWebAssembly = true` requires `esVersion` of at least '2022'; see https://www.scala-js.org/doc/project/webassembly.html")

    // TODO method withClosureCompiler in class ConfigExt is deprecated since 1.21.0:
    //  Support for the Google Closure Compiler is deprecated.
    //  It is off by default, and will eventually be removed.
    val fullOptimization: Boolean = optimization == Optimization.Full

    val moduleSplitStyleSJS: ModuleSplitStyleSJS = moduleSplitStyle match
      case ModuleSplitStyle.FewestModules   => ModuleSplitStyleSJS.FewestModules
      case ModuleSplitStyle.SmallestModules => ModuleSplitStyleSJS.SmallestModules
      case ModuleSplitStyle.SmallModulesFor => ModuleSplitStyleSJS.SmallModulesFor(smallModulesFor)

    val esVersionSJS: ESVersionSJS = esVersion match
      case ESVersion.ES2015 => ESVersionSJS.ES2015
      case ESVersion.ES2016 => ESVersionSJS.ES2016
      case ESVersion.ES2017 => ESVersionSJS.ES2017
      case ESVersion.ES2018 => ESVersionSJS.ES2018
      case ESVersion.ES2019 => ESVersionSJS.ES2019
      case ESVersion.ES2020 => ESVersionSJS.ES2020
      case ESVersion.ES2021 => ESVersionSJS.ES2021
      case ESVersion.ES2022 => ESVersionSJS.ES2022
      case ESVersion.ES2023 => ESVersionSJS.ES2023
      case ESVersion.ES2024 => ESVersionSJS.ES2024
      case ESVersion.ES2025 => ESVersionSJS.ES2025
      case ESVersion.ES2026 => ESVersionSJS.ES2026

    val linkerConfig: StandardConfig = StandardConfig()
      .withCheckIR(fullOptimization)
      .withSemantics(if fullOptimization then Semantics.Defaults.optimized else Semantics.Defaults)
      .withModuleKind(moduleKindSJS)
      .withClosureCompiler(fullOptimization && (moduleKind != ModuleKind.ESModule))
      .withModuleSplitStyle(moduleSplitStyleSJS)
      .withESFeatures(_
        .withESVersion(esVersionSJS)
        .withUseWebAssembly(useWebAssembly)
      )
      .withWasmFeatures(_.withUseJSPI(useJSPI))
      .withPrettyPrint(prettyPrint)

    val moduleInitializersSJS: Seq[ModuleInitializerSJS] = moduleInitializers
      .map(_.map(ScalaJSLink.toSJS))
      .orElse:
        // Tests use fixed entry point.
        if !isTest then None else Some(Seq(ModuleInitializerSJS.mainMethod(
            TestAdapterInitializer.ModuleClassName,
            TestAdapterInitializer.MainMethodName
        )))
      .getOrElse(Seq.empty) 

    debug(
      s"""\n
         |JSDirectory = $jsDirectory
         |reportFile = $reportTextFile
         |moduleInitializers = ${moduleInitializersSJS.map(ModuleInitializerSJS.fingerprint).mkString(", ")}
         |linkerConfig = $linkerConfig
         |""".stripMargin
    )

    jsDirectory.mkdirs()

    try
      val report: Report = Await.result(atMost = Duration.Inf, awaitable = PathIRContainer
        .fromClasspath(runtimeClasspath.map(_.toPath))
        .map(_._1)
        .flatMap((irContainers: Seq[IRContainer]) => StandardImpl.irFileCache().newCache.cached(irContainers))
        .flatMap((irFiles: Seq[IRFile]) => StandardImpl.linker(linkerConfig).link(
          irFiles = irFiles,
          moduleInitializers = moduleInitializersSJS,
          output = PathOutputDirectory(jsDirectory.toPath),
          logger = backendLogger
        ))
      )

      Files.write(reportTextFile, report.toString)
      Files.writeBytes(reportBinFile, Report.serialize(report))
    catch
      case e: LinkingException =>
        //e.printStackTrace()
        abort(s"ScalaJS link error: ${e.getMessage}")

object ScalaJSLink:
  private def toSJS(moduleInitializer: ModuleInitializer): ModuleInitializerSJS =
    val clazz: String = moduleInitializer.className
    val method: String = moduleInitializer.mainMethodName.getOrElse("main")
    val result: ModuleInitializerSJS =
      if moduleInitializer.mainMethodHasArgs
      then ModuleInitializerSJS.mainMethodWithArgs(clazz, method)
      else ModuleInitializerSJS.mainMethod(clazz, method)
    result.withModuleID(moduleInitializer.moduleId)
