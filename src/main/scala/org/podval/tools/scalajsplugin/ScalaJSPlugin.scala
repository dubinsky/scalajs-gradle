package org.podval.tools.scalajsplugin

import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.{Plugin, Project}
import org.gradle.api.plugins.scala.ScalaPlugin
import org.podval.tools.build.{Gradle, GradleClassPath, ScalaBackend, ScalaLibrary, ScalaPlatform}
import org.podval.tools.scalajsplugin.jvm.JvmDelegate
import org.podval.tools.scalajsplugin.scalajs.ScalaJSDelegate
import org.slf4j.{Logger, LoggerFactory}
import scala.jdk.CollectionConverters.IterableHasAsScala
import java.io.File
import javax.inject.Inject

// TODO use TaskProviders for all created tasks:
// use register() instead of create() and move task configuration into actions.
final class ScalaJSPlugin @Inject(
  objectFactory: ObjectFactory
) extends Plugin[Project]:
  import ScalaJSPlugin.logger

  override def apply(project: Project): Unit =
    project.getPluginManager.apply(classOf[ScalaPlugin])

    val isMixed: Boolean =
      val sourceRootPresent: Boolean = Set(
        JvmDelegate.sourceRoot,
        ScalaJSDelegate.sourceRoot
      )
        .exists(project.file(_).exists)

      val srcPresent: Boolean = project.file("src").exists

      if sourceRootPresent && srcPresent then
        logger.warn("ScalaJSPlugin: Both 'src' and platform-specific source roots are present! Ignoring 'src'.")

      sourceRootPresent

    val isJSDisabled: Boolean =
      Option(project.findProperty(ScalaJSPlugin.maiflaiProperty )).isDefined ||
      Option(project.findProperty(ScalaJSPlugin.disabledProperty)).exists(_.toString.toBoolean)

    val delegates: Seq[BackendDelegate] =
      if isJSDisabled
      then
        logger.info("ScalaJSPlugin: running in JVM mode; Scala.js is disabled.")
        Seq(JvmDelegate(project, objectFactory, isMixed = false))
      else
        if !isMixed
        then
          logger.info("ScalaJSPlugin: running in Scala.js mode.")
          Seq(ScalaJSDelegate(project, objectFactory, isMixed = false))
        else
          logger.info("ScalaJSPlugin: running in mixed mode.")
          Seq(
            JvmDelegate(project, objectFactory, isMixed = true),
//            ScalaJSDelegate(project, objectFactory, isMixed = true)
          )

    delegates.foreach(_.setUpProject())

    project.afterEvaluate: (project: Project) =>
      def getConfiguration(name: String): Iterable[File] = Gradle.getConfiguration(project, name).asScala

      val projectScalaLibrary: ScalaLibrary =
        ScalaLibrary.getFromConfiguration(project, JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)

      val projectScalaPlatform: ScalaPlatform =
        projectScalaLibrary.toPlatform(if isJSDisabled then ScalaBackend.Jvm else ScalaBackend.JS())

      val pluginScalaPlatform: ScalaPlatform =
        ScalaLibrary.getFromClasspath(GradleClassPath.collect(this)).toPlatform(ScalaBackend.Jvm)

      delegates
        .flatMap(_.dependencyRequirements(pluginScalaPlatform, projectScalaPlatform))
        .foreach(_.applyToConfiguration(project))

      delegates
        .foreach(_.configureProject(projectScalaPlatform))

      // Expanding plugin's classpath.
      delegates
        .flatMap(_.configurationToAddToClassPath)
        .foreach((configurationName: String) => GradleClassPath.addTo(this, getConfiguration(configurationName)))

      projectScalaLibrary.verify(
        ScalaLibrary.getFromClasspath(getConfiguration(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME))
      )

private object ScalaJSPlugin:
  private val logger: Logger = LoggerFactory.getLogger(ScalaJSPlugin.getClass)

  private val disabledProperty: String = "org.podval.tools.scalajs.disabled"
  private val maiflaiProperty : String = "com.github.maiflai.gradle-scalatest.mode"
