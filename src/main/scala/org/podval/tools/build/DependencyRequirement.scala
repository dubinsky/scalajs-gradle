package org.podval.tools.build

import org.gradle.api.artifacts.Configuration
import org.gradle.api.{GradleException, Project}
import org.podval.tools.util.Configurations

final class DependencyRequirement(
  dependency: JvmDependency,
  version: Version
):
  def apply(
    project: Project,
    configuration: Configuration,
    scalaLibrary: ScalaLibrary
  ): DependencyVersion = dependency.findInConfiguration(configuration).getOrElse:
    val dependencyNotation: String = dependency
      .withVersion(
        scalaLibrary = scalaLibrary,
        version = version
      )
      .dependencyNotation
    val what: String = s"dependency '$dependencyNotation' to configuration '${project.getName}.${configuration.getName}': ${dependency.name}."
    project.getLogger.info(s"Adding $what", null, null, null)
    Configurations.addDependency(project, configuration.getName, dependencyNotation)
    dependency.findInConfiguration(configuration).getOrElse(throw GradleException(s"Failed to add $what"))

object DependencyRequirement:
  // Arrays are used all the way to here for Scala 2.12 compatibility :(
  final class Many(
    requirements: Array[DependencyRequirement],
    scalaLibrary: ScalaLibrary,
    configurationName: String
  ):
    def apply(project: Project): Unit = if requirements.length > 0 then
      val configuration: Configuration = Configurations.configuration(project, configurationName)
      requirements.map(_.apply(
        project = project,
        scalaLibrary = scalaLibrary,
        configuration = configuration
      ))
