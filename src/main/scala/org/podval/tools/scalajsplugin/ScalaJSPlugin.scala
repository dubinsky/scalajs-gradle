package org.podval.tools.scalajsplugin

import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.scala.ScalaBasePlugin
import org.gradle.api.{Plugin, Project}
import org.gradle.api.plugins.scala.ScalaPlugin
import org.podval.tools.build.{Gradle, GradleClassPath, ScalaBackend, ScalaLibrary, ScalaPlatform}
import org.podval.tools.scalajsplugin.jvm.JvmDelegate
import org.podval.tools.scalajsplugin.scalajs.ScalaJSDelegate
import java.io.File
import scala.jdk.CollectionConverters.IterableHasAsScala

final class ScalaJSPlugin extends Plugin[Project]:
  override def apply(project: Project): Unit =
    project.getPluginManager.apply(classOf[ScalaPlugin])

//    println(project.getLayout.getProjectDirectory)
//    println(Gradle.getSourceSets(project).asScala)

    val isJSDisabled: Boolean =
      Option(project.findProperty(ScalaJSPlugin.maiflaiProperty )).isDefined ||
      Option(project.findProperty(ScalaJSPlugin.disabledProperty)).exists(_.toString.toBoolean)

    val delegate: BackendDelegate =
      if isJSDisabled
      then JvmDelegate(project)
      else ScalaJSDelegate(project)

    delegate.setUpProject()

    project.afterEvaluate: (project: Project) =>
      val projectScalaLibrary: ScalaLibrary = 
        ScalaLibrary.getFromConfiguration(project, JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)

      val projectScalaPlatform: ScalaPlatform =
        projectScalaLibrary.toPlatform(if isJSDisabled then ScalaBackend.Jvm else ScalaBackend.JS())

      delegate
        .dependencyRequirements(
          pluginScalaPlatform = ScalaLibrary.getFromClasspath(GradleClassPath.collect(this)).toPlatform(ScalaBackend.Jvm),
          projectScalaPlatform = projectScalaPlatform
        )
        .foreach(_.applyToConfiguration(project))

      delegate.configureProject(projectScalaPlatform)

      def getConfiguration(name: String): Iterable[File] = Gradle.getConfiguration(project, name).asScala

      // Expanding plugin's classpath.
      delegate
        .configurationToAddToClassPath
        .toSeq
        .foreach((configurationName: String) => GradleClassPath.addTo(this, getConfiguration(configurationName)))

      projectScalaLibrary.verify(
        ScalaLibrary.getFromClasspath(getConfiguration(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME))
      )
      
private object ScalaJSPlugin:
  private val disabledProperty: String = "org.podval.tools.scalajs.disabled"
  private val maiflaiProperty : String = "com.github.maiflai.gradle-scalatest.mode"
