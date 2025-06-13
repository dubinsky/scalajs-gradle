package org.podval.tools.build

import org.gradle.api.artifacts.Configuration
import org.gradle.api.{GradleException, Project}
import org.slf4j.{Logger, LoggerFactory}

object DependencyRequirement:
  private val logger: Logger = LoggerFactory.getLogger(DependencyRequirement.getClass)

final class DependencyRequirement(
  maker: DependencyMaker,
  version: PreVersion
):
  def applyToConfiguration(
    project: Project,
    configuration: Configuration,
    scalaVersion: ScalaVersion
  ): DependencyWithVersion =
    val findable: DependencyFindable[?] = maker.findable
    val dependency: Dependency = maker.dependency(scalaVersion)

    val result: DependencyWithVersion = findable.findInConfiguration(configuration).getOrElse:
      val toAdd: DependencyWithVersion = dependency.withVersion(version)
      DependencyRequirement.logger.info(
        s"Adding dependency $toAdd to configuration '${project.getName}.${configuration.getName}': ${maker.description}"
      )
      configuration
        .getDependencies
        .add(project.getDependencies.create(toAdd.dependencyNotation))
      findable
        .findInConfiguration(configuration)
        .getOrElse(throw GradleException(s"Failed to add dependency $toAdd to configuration ${configuration.getName}."))
    
    if maker.useExactVersionInVerifyRequired && result.version != version then
      DependencyRequirement.logger.warn(s"Found $result, but the project uses version $version.")
    
    result
