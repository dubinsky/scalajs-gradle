package org.podval.tools.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.scala.ScalaCompile
import org.slf4j.{Logger, LoggerFactory}
import scala.jdk.CollectionConverters.{IterableHasAsScala, ListHasAsScala, SeqHasAsJava}
import java.io.File

object ScalaCompiles:
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  def configure(project: Project, scalaCompileParameters: Seq[String]): Unit =
    val mainScalaCompile: ScalaCompile = getTask(project, isTest = false)
    ensureParameters(mainScalaCompile, scalaCompileParameters)
    addScalaCompilerPlugins(mainScalaCompile, Configurations.scalaCompilerPluginsName)

    val testScalaCompile: ScalaCompile = getTask(project, isTest = true)
    ensureParameters(testScalaCompile, scalaCompileParameters)
    addScalaCompilerPlugins(testScalaCompile, Configurations.scalaCompilerPluginsName)
    addScalaCompilerPlugins(testScalaCompile, Configurations.testScalaCompilerPluginsName)

  private def getTask(project: Project, isTest: Boolean): ScalaCompile =
    val taskName: String = Configurations.sourceSet(project, isTest).getCompileTaskName("scala")
    project
     .getTasks
     .withType(classOf[ScalaCompile])
     .findByName(taskName)

  private def ensureParameters(scalaCompile: ScalaCompile, toAdd: Seq[String]): Unit =
    val parameters: List[String] = Option(scalaCompile.getScalaCompileOptions.getAdditionalParameters) // nullable
      .map(_.asScala.toList)
      .getOrElse(List.empty)

    val parametersNew: List[String] = toAdd.foldLeft(parameters) {
      case (parameters, parameter) =>
        if parameters.contains(parameter) then parameters else
          logger.info(s"scalaCompileOptions.additionalParameters of the ${scalaCompile.getName} task: adding '$parameter'.")
          parameters :+ parameter
    }

    scalaCompile
      .getScalaCompileOptions
      .setAdditionalParameters(parametersNew.asJava)

  private def addScalaCompilerPlugins(scalaCompile: ScalaCompile, configurationName: String): Unit =
    val scalaCompilerPluginsConfiguration: Configuration = Configurations.configuration(scalaCompile.getProject, configurationName)

    // There seems to be no need to add `"-Xplugin:" + plugin.getPath` parameters:
    // just adding plugins to the list is sufficient.
    // I am not sure that even this is needed for the pre-existing `scalaCompilerPlugins` configuration.
    val scalaCompilerPlugins: Iterable[File] = scalaCompilerPluginsConfiguration.asScala
    if scalaCompilerPlugins.nonEmpty then
      logger.info(s"Adding ${scalaCompilerPluginsConfiguration.getName} to ${scalaCompile.getName}: $scalaCompilerPlugins.")
      scalaCompile.setScalaCompilerPlugins(Option(scalaCompile.getScalaCompilerPlugins)
        .map(_.plus(scalaCompilerPluginsConfiguration))
        .getOrElse(scalaCompilerPluginsConfiguration)
      )
