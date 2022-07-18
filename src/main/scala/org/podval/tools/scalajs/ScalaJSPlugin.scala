package org.podval.tools.scalajs

import org.gradle.api.{Plugin, Project}
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.scala.ScalaBasePlugin
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.tasks.SourceSet
import org.opentorah.build.Gradle.*
import org.opentorah.build.{Configurations, DependencyRequirement, DependencyVersion, ScalaLibrary}
import org.podval.tools.test.{Sbt, TestTaskScala}
import java.io.File
import scala.jdk.CollectionConverters.*

final class ScalaJSPlugin extends Plugin[Project]:
  override def apply(project: Project): Unit =
    val isScalaJSDisabled: Boolean = ScalaJSPlugin.isScalaJSDisabled(project)

    project.getPluginManager.apply(classOf[org.gradle.api.plugins.scala.ScalaPlugin])

    ScalaJSPlugin.createInternalConfiguraton(
      project,
      name = Sbt.configurationName,
      description = "sbt dependencies used by the ScalaJS plugin."
    )

    if isScalaJSDisabled then
      project.getTasks.replace("test", classOf[TestTaskScala])
    else
      ScalaJSPlugin.createInternalConfiguraton(
        project,
        name = ScalaJS.configurationName,
        description = "ScalaJS dependencies used by the ScalaJS plugin."
      )

      val link: LinkTask.Main = project.getTasks.create("link", classOf[LinkTask.Main])
      project.getTasks.create("run", classOf[RunTask]).dependsOn(link)

      val linkTest: LinkTask.Test = project.getTasks.create("linkTest", classOf[LinkTask.Test])
      project.getTasks.replace("test", classOf[TestTask]).dependsOn(linkTest)

    project.afterEvaluate((project: Project) =>
      val implementation: Configuration = project.getConfiguration(Configurations.implementationConfiguration)

      val pluginScalaLibrary: ScalaLibrary = ScalaLibrary.getFromClasspath(collectClassPath(getClass.getClassLoader))
      val projectScalaLibrary: ScalaLibrary = ScalaLibrary.getFromConfiguration(implementation)

      val sbtRequirements: Seq[DependencyRequirement] = Sbt.requirements(
        pluginScalaLibrary,
        Sbt.getVersion(project.getConfiguration(ScalaBasePlugin.ZINC_CONFIGURATION_NAME))
      )

      val scalaJSRequirements: Seq[DependencyRequirement] = if isScalaJSDisabled then Seq.empty else
        ScalaJS.requirements(
          pluginScalaLibrary,
          projectScalaLibrary,
          scalaJSVersion = ScalaJS.getVersion(implementation)
        )

      val requirements: Seq[DependencyRequirement] = sbtRequirements ++ scalaJSRequirements

      requirements.foreach(_.applyToConfiguration(project))

      if !isScalaJSDisabled && projectScalaLibrary.isScala3 then
        configureScalaCompile(SourceSet.MAIN_SOURCE_SET_NAME, project)
        configureScalaCompile(SourceSet.TEST_SOURCE_SET_NAME, project)

      requirements.foreach(_.applyToClassPath(project))

      projectScalaLibrary.verify(
        ScalaLibrary.getFromClasspath(project.getConfiguration(Configurations.implementationClassPath).asScala)
      )
    )

  private def configureScalaCompile(sourceSetName: String, project: Project): Unit =
    val scalaCompile: ScalaCompile = project.getScalaCompile(sourceSetName)
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

  private def isScalaJSDisabled(project: Project): Boolean =
    Option(project.findProperty(maiflaiProperty )).isDefined ||
    Option(project.findProperty(disabledProperty)).exists(_.toString.toBoolean)

  private def createInternalConfiguraton(
    project: Project,
    name: String,
    description: String
  ): Configuration =
    val result: Configuration = project.getConfigurations.create(name)
    result.setVisible(false)
    result.setCanBeConsumed(false)
    result.setDescription(description)
    result
