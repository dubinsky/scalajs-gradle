package org.podval.tools.scalajs

import org.gradle.api.{DefaultTask, Plugin, Project}
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.provider.Property
import org.gradle.api.tasks.{SourceSet, TaskAction}
import org.scalajs.linker.{PathIRContainer, PathOutputDirectory, StandardImpl}
import org.scalajs.linker.interface.{IRContainer, IRFile, IRFileCache, Linker, ModuleInitializer, ModuleKind, ModuleSplitStyle, OutputDirectory, Report, Semantics, StandardConfig}
import org.scalajs.logging.{Level, Logger, ScalaConsoleLogger}

import java.io.File
import java.nio.file.Path
import scala.concurrent.{Await, Future}
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

    final def getMainModuleInitializer: ModuleInitializer =
      val mainClass : String = getOrElse(getMainClass , throw new IllegalArgumentException("'scalajs.mainClass' is not set"))
      val mainMethod: String = getOrElse(getMainMethod, "main")
      if getOrElse(getMainMethodHasArgs, true)
      then ModuleInitializer.mainMethodWithArgs(mainClass, mainMethod)
      else ModuleInitializer.mainMethod        (mainClass, mainMethod)

    def getModuleKind: Property[String]

    final def moduleKind: ModuleKind = byName(getModuleKind, ModuleKind.NoModule, ModuleKind.All)

    def getModuleSplitStyle: Property[String]

    final def moduleSplitStyle: ModuleSplitStyle = byName(getModuleSplitStyle, ModuleSplitStyle.FewestModules,
      List(ModuleSplitStyle.FewestModules, ModuleSplitStyle.SmallestModules))

  // TODO
//    optimizer = true,
//    jsHeader = "",
//    parallel = true,
//    sourceMap = true,
//    relativizeSourceMapBase = None,
//    closureCompilerIfAvailable = false,
//    prettyPrint = false,
//    batchMode = false,
//    maxConcurrentWrites = 50

  private def getOrElse[T](property: Property[T], default: => T): T =
    if !property.isPresent then default else property.get

  private def byName[T](property: Property[String], default: T, all: List[T]): T =
    val name: String = getOrElse(property, default.toString)
    all.find(_.toString == name).get

  class FastLinkTask extends DefaultTask:
    setGroup("build")
    setDescription("Link ScalaJs - fast")
    @TaskAction def execute(): Unit = link(getProject, fullOptimization = false)

  class FullLinkTask extends DefaultTask:
    setGroup("build")
    setDescription("Link ScalaJs - full optimization")
    @TaskAction def execute(): Unit = link(getProject, fullOptimization = true)

  private def link(project: Project, fullOptimization: Boolean): Unit =
    val extension: Extension = project.getExtensions.getByType(classOf[Extension])

    val outputDir: File = File(project.getBuildDir, "js")
    outputDir.mkdirs()

    val linkerConfig: StandardConfig = StandardConfig()
      .withCheckIR  (fullOptimization)
      .withSemantics((semantics: Semantics) => if fullOptimization then semantics.optimized else semantics)
      .withClosureCompiler(fullOptimization && extension.moduleKind != ModuleKind.ESModule)

      .withModuleKind      (extension.moduleKind)
      .withModuleSplitStyle(extension.moduleSplitStyle)

    // Same as scalaJSModuleInitializers in sbt, add if needed.
    // Without the initializer, no JavaScript is emitted!
    val moduleInitializers: Seq[ModuleInitializer] = Seq(
      extension.getMainModuleInitializer
    )

    val report: Report = Await.result(atMost = Duration.Inf, awaitable = PathIRContainer
      .fromClasspath(getMainSourceSet(project).getRuntimeClasspath.getFiles.asScala.toSeq.map(_.toPath))
      .map(_._1)
      .flatMap((irContainers: Seq[IRContainer]) => StandardImpl.irFileCache().newCache.cached(irContainers))
      .flatMap((irFiles     : Seq[IRFile]     ) => StandardImpl.linker(linkerConfig).link(
        irFiles,
        moduleInitializers,
        PathOutputDirectory(outputDir.toPath),
        ScalaConsoleLogger(Level.Debug) // TODO GradleLogger(logger)
      ))
    )

  private def getMainSourceSet(project: Project): SourceSet =
    Option(project.getConvention.findPlugin(classOf[JavaPluginConvention]))
      .get
      .getSourceSets
      .getByName(SourceSet.MAIN_SOURCE_SET_NAME)

  // TODO use this after correcting the levels
  final class GradleLogger(logger: org.gradle.api.logging.Logger) extends Logger:
    override def trace(t: => Throwable): Unit = logger.error("ScalaJS Link Error", t)
    override def log(level: Level, message: => String): Unit = level match
      case Level.Warn  => logger.warn (message)
      case Level.Error => logger.error(message)
      case Level.Info  => logger.info (message, null, null, null)
      case Level.Debug => logger.debug(message, null, null, null)
