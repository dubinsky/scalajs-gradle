package org.podval.tools.scalajs

import org.gradle.api.{DefaultTask, Plugin, Project}
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.provider.Property
import org.gradle.api.tasks.{SourceSet, TaskAction}
import org.scalajs.linker.{PathIRContainer, PathOutputDirectory, StandardImpl}
import org.scalajs.linker.interface.{IRContainer, IRFile, ModuleInitializer, ModuleKind, ModuleSplitStyle, Report,
  Semantics, StandardConfig}
import org.scalajs.logging.{Level, Logger}

import java.io.File
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters.*

final class ScalaJSPlugin extends Plugin[Project]:
  override def apply(project: Project): Unit =
    project.getExtensions.create("scalajs", classOf[ScalaJSPlugin.Extension])
    project.getTasks.create("fastLinkJS", classOf[ScalaJSPlugin.FastLinkTask])
    project.getTasks.create("fullLinkJS", classOf[ScalaJSPlugin.FullLinkTask])

object ScalaJSPlugin:

  abstract class Extension:
    def getMainClass: Property[String]
    def getMainMethod: Property[String]
    def getMainMethodHasArgs: Property[Boolean]

    final def mainModuleInitializer: Option[ModuleInitializer] = optional(getMainClass).map { (mainClass: String) =>
      val mainMethod: String = getOrElse(getMainMethod, "main")
      if getOrElse(getMainMethodHasArgs, false)
      then ModuleInitializer.mainMethodWithArgs(mainClass, mainMethod)
      else ModuleInitializer.mainMethod(mainClass, mainMethod)
    }

    def getOutputDirectory: Property[File]
    final def outputDirectory(project: Project): File = getOrElse(getOutputDirectory, File(project.getBuildDir, "js"))

    def getModuleKind: Property[String]
    def getModuleSplitStyle: Property[String]
    def getPrettyPrint: Property[Boolean]
    // TODO more properties for the other withXXX()

    private val moduleSplitStyles: List[ModuleSplitStyle] = List(ModuleSplitStyle.FewestModules, ModuleSplitStyle.SmallestModules)

    final def linkerConfig(fullOptimization: Boolean): StandardConfig =
      val moduleKind: ModuleKind = byName(getModuleKind, ModuleKind.NoModule, ModuleKind.All)
      StandardConfig()
        .withCheckIR(fullOptimization)
        .withSemantics(if fullOptimization then Semantics.Defaults.optimized else Semantics.Defaults)
        .withClosureCompiler(fullOptimization && (moduleKind != ModuleKind.ESModule))
        .withModuleKind(moduleKind)
        .withModuleSplitStyle(byName(getModuleSplitStyle, ModuleSplitStyle.FewestModules, moduleSplitStyles))
        .withPrettyPrint(getOrElse(getPrettyPrint, false))

  class FastLinkTask extends LinkTask(fullOptimization = false, descriptionSuffix = "fast"             )
  class FullLinkTask extends LinkTask(fullOptimization = true , descriptionSuffix = "full optimization")

  abstract class LinkTask(fullOptimization: Boolean, descriptionSuffix: String) extends DefaultTask:
    setGroup("build")
    setDescription(s"Link ScalaJS - $descriptionSuffix")

    private def getExtension: Extension = getProject.getExtensions.getByType(classOf[Extension])
    private def getRuntimeClassPath: FileCollection = getMainSourceSet(getProject).getRuntimeClasspath
    private def info(message: String): Unit = getProject.getLogger.info(message, null, null, null)

    getProject.afterEvaluate { (project: Project) =>
      getDependsOn.add(getClassesTask(project))
      getInputs.files(getRuntimeClassPath)
      getOutputs.dir(getExtension.outputDirectory(project))
      () // return Unit to help the compiler find the correct overload
    }

    @TaskAction def execute(): Unit =
      val extension: Extension = getExtension
      val outputDirectory: File = extension.outputDirectory(getProject)

      // Without the initializer, no JavaScript is emitted!
      val mainModuleInitializer: Option[ModuleInitializer] = extension.mainModuleInitializer
      val linkerConfig: StandardConfig = extension.linkerConfig(fullOptimization)

      info(
        s"""ScalaJSPlugin:
           |outputDirectory = $outputDirectory
           |mainModuleInitializer = ${mainModuleInitializer.map(ModuleInitializer.fingerprint)}
           |linkerConfig = $linkerConfig
           |""".stripMargin,
      )

      outputDirectory.mkdirs()

      val report: Report = Await.result(atMost = Duration.Inf, awaitable = PathIRContainer
        .fromClasspath(getRuntimeClassPath.getFiles.asScala.toSeq.map(_.toPath))
        .map(_._1)
        .flatMap((irContainers: Seq[IRContainer]) => StandardImpl.irFileCache().newCache.cached(irContainers))
        .flatMap((irFiles     : Seq[IRFile]     ) => StandardImpl.linker(linkerConfig).link(
          irFiles = irFiles,
          moduleInitializers = mainModuleInitializer.toSeq,
          output = PathOutputDirectory(outputDirectory.toPath),
          logger = GradleLogger(getProject)
        ))
      )

      info(report.toString)

  private def getMainSourceSet(project: Project): SourceSet = project
    .getConvention
    .findPlugin(classOf[JavaPluginConvention])
    .getSourceSets
    .getByName(SourceSet.MAIN_SOURCE_SET_NAME)

  private def getClassesTask(project: Project) = project
    .getTasks
    .findByName(getMainSourceSet(project).getClassesTaskName)

  private def optional[T](property: Property[T]): Option[T] =
    if !property.isPresent then None else Some(property.get)

  private def getOrElse[T](property: Property[T], default: => T): T =
    if !property.isPresent then default else property.get

  private def byName[T](property: Property[String], default: => T, all: List[T]): T =
    if !property.isPresent then default else all.find(_.toString == property.get).get

  final class GradleLogger(project: Project) extends Logger:
    override def trace(t: => Throwable): Unit =
      project.getLogger.error("ScalaJS Linker Error", t)
    override def log(level: Level, message: => String): Unit =
      project.getLogger.log(scalajs2gradleLevel(level), "ScalaJS Linker: " + message)

  private def scalajs2gradleLevel(level: Level): LogLevel = level match
    case Level.Error => LogLevel.ERROR
    case Level.Warn  => LogLevel.WARN
    case Level.Info  => LogLevel.INFO
    case Level.Debug => LogLevel.DEBUG
