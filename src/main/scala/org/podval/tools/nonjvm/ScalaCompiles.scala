package org.podval.tools.nonjvm

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.scala.ScalaCompile
import org.podval.tools.util.{Configurations, Tasks}
import org.slf4j.{Logger, LoggerFactory}
import scala.jdk.CollectionConverters.{IterableHasAsScala, ListHasAsScala, SeqHasAsJava}
import java.io.File

object ScalaCompiles:
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  // Scala 3.8+ publishes a standard library built with JDK 17.
  // The compiler requires an explicit release when the running JDK is newer.
  val javaReleaseParameter: String = "-release:17"

  def javaReleaseToAdd(parameters: Seq[String], scala3LibraryCompiledByScala3: Boolean): Option[String] =
    if !scala3LibraryCompiledByScala3 then None
    else if parameters.exists(isJavaRelease) then None
    else Some(javaReleaseParameter)

  private def isJavaRelease(parameter: String): Boolean =
    parameter.startsWith("-release") || parameter.startsWith("-java-output-version")

  def configureJavaRelease(project: Project, scala3LibraryCompiledByScala3: Boolean): Unit =
    Tasks.configureEach(
      project,
      classOf[ScalaCompile],
      (scalaCompile: ScalaCompile) =>
        javaReleaseToAdd(parametersOf(scalaCompile), scala3LibraryCompiledByScala3)
          .foreach(parameter => ensureParameters(scalaCompile, Seq(parameter)))
    )

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

  private def parametersOf(scalaCompile: ScalaCompile): List[String] =
    Option(scalaCompile.getScalaCompileOptions.getAdditionalParameters) // nullable
      .map(_.asScala.toList)
      .getOrElse(List.empty)

  private def ensureParameters(scalaCompile: ScalaCompile, toAdd: Seq[String]): Unit =
    val parameters: List[String] = parametersOf(scalaCompile)

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
