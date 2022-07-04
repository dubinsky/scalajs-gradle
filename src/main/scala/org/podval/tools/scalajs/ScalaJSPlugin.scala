package org.podval.tools.scalajs

import org.gradle.api.{Plugin, Project}
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.tasks.SourceSet
import org.opentorah.build.Gradle.*
import org.podval.tools.test.GradleUtil.*
import org.opentorah.build.{DependencyRequirement, ScalaLibrary}
import org.podval.tools.test.ScalaTestTask
import scala.jdk.CollectionConverters.*

final class ScalaJSPlugin extends Plugin[Project]:
  override def apply(project: Project): Unit =
    project.getPluginManager.apply(classOf[org.gradle.api.plugins.scala.ScalaPlugin])

    if ScalaJSPlugin.isScalaJSDisabled(project) then
      project.getTasks.replace("test", classOf[ScalaTestTask])
    else
      val configuration: Configuration = project.getConfigurations.create(ScalaJS.configurationName)
      configuration.setVisible(false)
      configuration.setCanBeConsumed(false)
      configuration.setDescription("ScalaJS dependencies used by the ScalaJS plugin.")
      
      val link: Link.Main = project.getTasks.create("link", classOf[Link.Main])
      project.getTasks.create("run", classOf[Run]).dependsOn(link)

      val linkTest: Link.Test = project.getTasks.create("linkTest", classOf[Link.Test])
      project.getTasks.replace("test", classOf[Test]).dependsOn(linkTest)

      project.afterEvaluate((project: Project) =>
        val scalaLibrary: ScalaLibrary = ScalaLibrary.getFromConfiguration(project)
        val requirements: Seq[DependencyRequirement] =
          scalaJSlibraryRequirement(scalaLibrary).toSeq ++ ScalaJS.forVersions(scalaLibrary, project)
        requirements.foreach(_.applyToConfiguration(project))

        if scalaLibrary.isScala3 then
          configureScalaCompile(SourceSet.MAIN_SOURCE_SET_NAME, project)
          configureScalaCompile(SourceSet.TEST_SOURCE_SET_NAME, project)

        requirements.foreach(_.applyToClasspath(project))
        scalaLibrary.verifyFromClasspath(project)
      )

  private def configureScalaCompile(sourceSetName: String, project: Project): Unit =
    val scalaCompile: ScalaCompile = project.getScalaCompileForSourceSet(sourceSetName)
    val parameters: List[String] = Option(scalaCompile.getScalaCompileOptions.getAdditionalParameters) // Note: nullable
      .map(_.asScala.toList)
      .getOrElse(List.empty)

    if !parameters.contains("-scalajs") then
      project.getLogger.info(s"scalaCompileOptions.additionalParameters of the $sourceSetName ScalaCompile task: adding '-scalajs'",
        null, null, null)
      scalaCompile
        .getScalaCompileOptions
        .setAdditionalParameters((parameters :+ "-scalajs").asJava)

object ScalaJSPlugin:
  private val disabledProperty: String = "org.podval.tools.scalajs.disabled"
  private val maiflaiProperty : String = "com.github.maiflai.gradle-scalatest.mode"

  def isScalaJSDisabled(project: Project): Boolean =
    Option(project.findProperty(maiflaiProperty )).isDefined ||
    Option(project.findProperty(disabledProperty)).exists(_.toString == "true") // TODO parse the Boolean
