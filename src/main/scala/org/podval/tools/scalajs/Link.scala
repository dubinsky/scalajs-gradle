package org.podval.tools.scalajs

import org.gradle.api.logging.Logger
import org.gradle.api.provider.Property
import org.gradle.api.GradleException
import org.opentorah.build.Gradle.*
import org.opentorah.util.Files
import org.scalajs.linker.interface.{IRContainer, IRFile, LinkingException, ModuleInitializer, ModuleKind,
  ModuleSplitStyle, Report, Semantics, StandardConfig}
import org.scalajs.linker.{PathIRContainer, PathOutputDirectory, StandardImpl}
import org.scalajs.testing.adapter.TestAdapterInitializer
import sbt.io.IO
import java.io.File
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters.*

object Link:
  private given CanEqual[ModuleKind, ModuleKind] = CanEqual.derived

  def moduleKind(property: Property[String]): ModuleKind =
    property.byName(ModuleKind.NoModule, ModuleKind.All)

  def link(
    moduleKind: ModuleKind,
    reportBinFile: File,
    jsDirectory: File,
    taskName: String,
    logger: Logger,
    runtimeClassPath: java.util.Set[File],
    moduleInitializerProperties: Option[java.util.Set[ModuleInitializerProperties]],
    fullOptimization: Boolean,
    moduleSplitStyleProperty: Property[String],
    prettyPrintProperty: Property[Boolean],
    reportTextFile: File,
  ): Unit =

    val moduleInitializers: Seq[ModuleInitializer] = moduleInitializerProperties
      .map(_.asScala.toSeq.map(toModuleInitializer))
      .getOrElse(
        // Note: tests use fixed entry point
        Seq(ModuleInitializer.mainMethod(
          TestAdapterInitializer.ModuleClassName,
          TestAdapterInitializer.MainMethodName
        ))
      )

    // Note: if moved into the caller breaks class loading
    val linkerConfig: StandardConfig = StandardConfig()
      .withCheckIR(fullOptimization)
      .withSemantics(if fullOptimization then Semantics.Defaults.optimized else Semantics.Defaults)
      .withModuleKind(moduleKind)
      .withClosureCompiler(fullOptimization && (moduleKind == ModuleKind.ESModule))
      .withModuleSplitStyle(moduleSplitStyleProperty.byName(ModuleSplitStyle.FewestModules, moduleSplitStyles))
      .withPrettyPrint(prettyPrintProperty.getOrElse(false))

    logger.info(
      s"""ScalaJSPlugin ${taskName}:
         |JSDirectory = $jsDirectory
         |reportFile = $reportTextFile
         |moduleInitializers = ${moduleInitializers.map(ModuleInitializer.fingerprint).mkString(", ")}
         |linkerConfig = $linkerConfig
         |""".stripMargin,
      null, null, null)

    jsDirectory.mkdirs()

    try
      val report: Report = Await.result(atMost = Duration.Inf, awaitable = PathIRContainer
        .fromClasspath(runtimeClassPath.asScala.toSeq.map(_.toPath))
        .map(_._1)
        .flatMap((irContainers: Seq[IRContainer]) => StandardImpl.irFileCache.newCache.cached(irContainers))
        .flatMap((irFiles: Seq[IRFile]) => StandardImpl.linker(linkerConfig).link(
          irFiles = irFiles,
          moduleInitializers = moduleInitializers,
          output = PathOutputDirectory(jsDirectory.toPath),
          logger = ScalaJSLogger(taskName, logger)
        ))
      )

      Files.write(file = reportTextFile, content = report.toString())
      IO.write(reportBinFile, Report.serialize(report))
    catch
      case e: LinkingException => throw GradleException("ScalaJS link error", e)

  private val moduleSplitStyles: List[ModuleSplitStyle] = List(
    ModuleSplitStyle.FewestModules,
    ModuleSplitStyle.SmallestModules
  )

  private def toModuleInitializer(properties: ModuleInitializerProperties): ModuleInitializer =
    val clazz: String = properties.getClassName.get
    val method: String = properties.getMainMethodName.getOrElse("main")
    val result: ModuleInitializer =
      if properties.getMainMethodHasArgs.getOrElse(false)
      then ModuleInitializer.mainMethodWithArgs(clazz, method)
      else ModuleInitializer.mainMethod(clazz, method)
    result.withModuleID(properties.getName)
