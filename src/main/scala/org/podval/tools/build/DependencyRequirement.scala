package org.podval.tools.build

import org.gradle.api.artifacts.Configuration
import org.gradle.api.{GradleException, Project}
import org.podval.tools.gradle.Configurations

final class DependencyRequirement(
  maker: DependencyMaker,
  version: PreVersion
):
  def apply(
    project: Project,
    configuration: Configuration,
    scalaLibrary: ScalaLibrary
  ): Dependency#WithVersion =
    val findable: DependencyFindable[?] = maker.findable
    val dependency: Dependency = maker.dependency(scalaLibrary)

    val result: Dependency#WithVersion = findable.findInConfiguration(configuration).getOrElse:
      val toAdd: Dependency#WithVersion = dependency.withVersion(version)
      val display: String = s"dependency $toAdd to configuration '${project.getName}.${configuration.getName}': ${maker.description}."
      project.getLogger.info(s"Adding $display", null, null, null)
      Configurations.addDependency(project, configuration.getName, toAdd.dependencyNotation)
      findable
        .findInConfiguration(configuration)
        .getOrElse(throw GradleException(s"Failed to add $display"))
    
    if maker.isDependencyRequirementVersionExact && result.version != version then
      project.getLogger.warn(s"Found $result, but the project uses version $version.")
    
    result

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
