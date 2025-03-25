package org.podval.tools.scalajsplugin

import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.scala.ScalaBasePlugin
import org.gradle.api.{Plugin, Project}
import org.podval.tools.build.{Gradle, GradleClassPath, ScalaBackend, ScalaLibrary, ScalaPlatform}
import org.podval.tools.scalajsplugin.jvm.JvmDelegate
import org.podval.tools.scalajsplugin.scalajs.ScalaJSDelegate
import java.io.File
import scala.jdk.CollectionConverters.IterableHasAsScala

final class ScalaJSPlugin extends Plugin[Project]:
  override def apply(project: Project): Unit =
    project.getPluginManager.apply(classOf[org.gradle.api.plugins.scala.ScalaPlugin])

    val isJSDisabled: Boolean =
      Option(project.findProperty(ScalaJSPlugin.maiflaiProperty )).isDefined ||
      Option(project.findProperty(ScalaJSPlugin.disabledProperty)).exists(_.toString.toBoolean)

    val delegate: BackendDelegate = if isJSDisabled then JvmDelegate() else ScalaJSDelegate()

    delegate.beforeEvaluate(project)

    project.afterEvaluate: (project: Project) =>
      val projectScalaLibrary: ScalaLibrary = 
        ScalaLibrary.getFromConfiguration(Gradle.getConfiguration(project, JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME))

      val projectScalaPlatform: ScalaPlatform =
        projectScalaLibrary.toPlatform(if isJSDisabled then ScalaBackend.Jvm else ScalaBackend.JS())

      delegate.configureProject(
        project,
        projectScalaPlatform
      )

      delegate
        .dependencyRequirements(
          project,
          pluginScalaPlatform = ScalaLibrary.getFromClasspath(GradleClassPath.collect(this)).toPlatform(ScalaBackend.Jvm),
          projectScalaPlatform = projectScalaPlatform
        )
        .foreach(_.applyToConfiguration(project))

      def getConfiguration(name: String): Iterable[File] = Gradle.getConfiguration(project, name).asScala

      projectScalaLibrary.verify(
        ScalaLibrary.getFromClasspath(getConfiguration(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME))
      )

      // Expanding plugin's classpath.
      delegate
        .configurationToAddToClassPath
        .toSeq
        .foreach((configurationName: String) => GradleClassPath.addTo(this, getConfiguration(configurationName)))

private object ScalaJSPlugin:
  private val disabledProperty: String = "org.podval.tools.scalajs.disabled"
  private val maiflaiProperty : String = "com.github.maiflai.gradle-scalatest.mode"
