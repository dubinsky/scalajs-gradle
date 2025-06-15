package org.podval.tools.build

import org.gradle.api.artifacts.Configuration
import org.gradle.api.{GradleException, Project}
import org.slf4j.{Logger, LoggerFactory}

final class DependencyRequirement(
  maker: Dependency.Maker,
  version: PreVersion
):
  def applyToConfiguration(
    project: Project,
    configuration: Configuration,
    scalaVersion: ScalaVersion
  ): Dependency.WithVersion =
    val findable: FindableDependency[?] = maker.findable
    val dependency: Dependency = maker.dependency(scalaVersion)

    val result: Dependency.WithVersion = findable.findInConfiguration(configuration).getOrElse:
      val toAdd: Dependency.WithVersion = dependency.withVersion(version)
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

object DependencyRequirement:
  private val logger: Logger = LoggerFactory.getLogger(DependencyRequirement.getClass)
