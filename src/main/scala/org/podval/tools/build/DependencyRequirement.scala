package org.podval.tools.build

import org.gradle.api.artifacts.Configuration
import org.gradle.api.{GradleException, Project}
import org.podval.tools.gradle.Configurations

final class DependencyRequirement(
  dependency: Dependency,
  version: PreVersion
):
  def apply(
    project: Project,
    configuration: Configuration,
    scalaLibrary: ScalaLibrary
  ): Dependency.WithVersion = dependency.findInConfiguration(configuration).getOrElse:
    val dependencyNotation: String = dependency.dependencyNotation(
      backend = dependency.scalaBackend,
      scalaLibrary = scalaLibrary,
      version = Some(version)
    )
    val what: String = s"dependency '$dependencyNotation' to configuration '${project.getName}.${configuration.getName}': ${dependency.description}."
    project.getLogger.info(s"Adding $what", null, null, null)
    Configurations.addDependency(project, configuration.getName, dependencyNotation)
    dependency.findInConfiguration(configuration).getOrElse(throw GradleException(s"Failed to add $what"))

object DependencyRequirement:
  // Arrays are used all the way to here for Scala 2.12 compatibility :(
  final class Many(
    dependencyRequirements: Array[DependencyRequirement],
    scalaLibrary: ScalaLibrary,
    configurationName: String
  ):
    def apply(project: Project): Unit = if dependencyRequirements.length > 0 then
      val configuration: Configuration = Configurations.configuration(project, configurationName)
      dependencyRequirements.map(_.apply(
        project = project,
        scalaLibrary = scalaLibrary,
        configuration = configuration
      ))
