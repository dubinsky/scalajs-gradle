package org.podval.tools.build

import org.gradle.api.artifacts.Configuration
import org.gradle.api.{GradleException, Project}
import org.podval.tools.gradle.Configurations
import org.slf4j.{Logger, LoggerFactory}

final class DependencyRequirement(
  maker: DependencyMaker,
  version: PreVersion
):
  def apply(
    project: Project,
    configuration: Configuration,
    scalaVersion: ScalaVersion
  ): Dependency#WithVersion =
    val findable: DependencyFindable[?] = maker.findable
    val dependency: Dependency = maker.dependency(scalaVersion)

    val result: Dependency#WithVersion = findable.findInConfiguration(configuration).getOrElse:
      val toAdd: Dependency#WithVersion = dependency.withVersion(version)
      val display: String = s"dependency $toAdd to configuration '${project.getName}.${configuration.getName}': ${maker.description}."
      DependencyRequirement.logger.info(s"Adding $display")
      Configurations.addDependency(project, configuration.getName, toAdd.dependencyNotation)
      findable
        .findInConfiguration(configuration)
        .getOrElse(throw GradleException(s"Failed to add $display"))
    
    if maker.isDependencyRequirementVersionExact && result.version != version then
      DependencyRequirement.logger.warn(s"Found $result, but the project uses version $version.")
    
    result

object DependencyRequirement:
  private val logger: Logger = LoggerFactory.getLogger(DependencyRequirement.getClass)

  // Arrays are used all the way to here for Scala 2.12 compatibility :(
  final class Many(
    dependencyRequirements: Array[DependencyRequirement],
    scalaVersion: ScalaVersion,
    configurationName: String
  ):
    def apply(project: Project): Unit = if dependencyRequirements.length > 0 then
      val configuration: Configuration = Configurations.configuration(project, configurationName)
      dependencyRequirements.map(_.apply(
        project = project,
        scalaVersion = scalaVersion,
        configuration = configuration
      ))
