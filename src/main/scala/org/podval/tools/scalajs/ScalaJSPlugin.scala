package org.podval.tools.scalajs

import org.gradle.api.{DefaultTask, Plugin, Project}
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.{SourceSet, TaskAction}
import org.scalajs.linker.{PathIRContainer, PathOutputDirectory, StandardImpl}
import org.scalajs.linker.interface.{IRContainer, IRFile, IRFileCache, Linker, ModuleInitializer, ModuleKind,
  OutputDirectory, Report, StandardConfig}
import org.scalajs.logging.{Level, Logger, ScalaConsoleLogger}

import java.io.File
import java.nio.file.Path
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters.*

// Inspired by https://stackoverflow.com/questions/65092014/build-compile-latest-salajs-1-3-using-gradle-on-a-windows-machine
final class ScalaJSPlugin extends Plugin[Project]:

  override def apply(project: Project): Unit =
    project.getTasks.create("fastLinkJS", classOf[ScalaJSPlugin.FastLinkTask])


object ScalaJSPlugin:

  class FastLinkTask extends DefaultTask:
    setGroup("build")
    setDescription("Link ScalaJs - fast")
    @TaskAction def execute(): Unit = link(getProject)

  def link(project: Project): Unit =
    val classpath: Seq[File] = Option(project.getConvention.findPlugin(classOf[JavaPluginConvention])).get
      .getSourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
      .getRuntimeClasspath
      .getFiles
      .asScala
      .toSeq

    val outputDir: File = File(project.getBuildDir, "js")
    outputDir.mkdirs()

    val linkerConfig: StandardConfig = StandardConfig() // look at the API of this, lots of options.
      .withModuleKind(ModuleKind.ESModule)

    // Same as scalaJSModuleInitializers in sbt, add if needed.
    val moduleInitializers: Seq[ModuleInitializer] = Seq()

    val result: Future[Report] = PathIRContainer
      .fromClasspath(classpath.map(_.toPath))
      .map(_._1)
      .flatMap((irContainers: Seq[IRContainer]) => StandardImpl.irFileCache().newCache.cached(irContainers))
      .flatMap((irFiles: Seq[IRFile]) => StandardImpl.linker(linkerConfig).link(
        irFiles,
        moduleInitializers,
        PathOutputDirectory(outputDir.toPath),
        ScalaConsoleLogger(Level.Debug) // TODO GradleLogger(logger)
      ))

    val report: Report = Await.result(result, Duration.Inf)

  final class GradleLogger(logger: org.gradle.api.logging.Logger) extends Logger:
    override def log(level: Level, message: => String): Unit = level match
      case Level.Warn  => logger.warn (message)
      case Level.Error => logger.error(message)
      case Level.Info  => logger.info (message, null, null, null)
      case Level.Debug => logger.debug(message, null, null, null)

    override def trace(t: => Throwable): Unit = logger.error("ScalaJS Link Error", t)
