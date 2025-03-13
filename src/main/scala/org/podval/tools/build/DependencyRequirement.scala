package org.podval.tools.build

import org.gradle.api.artifacts.Configuration
import org.gradle.api.{GradleException, Project}
import org.slf4j.{Logger, LoggerFactory}

final class DependencyRequirement(
  findable: FindableDependency[?],
  dependency: Dependency,
  version: Version,
  reason: String,
  configurationName: String,
  isVersionExact: Boolean
):
  def applyToConfiguration(project: Project): Dependency.WithVersion =
    val configuration: Configuration = Gradle.getConfiguration(project, configurationName)
    val result: Dependency.WithVersion = findable.findInConfiguration(configuration).getOrElse:
      val toAdd: Dependency.WithVersion = dependency.withVersion(version)
      DependencyRequirement.logger.info(s"Adding dependency $toAdd to the $configuration $reason")
      configuration
        .getDependencies
        .add(project.getDependencies.create(toAdd.dependencyNotation))
      findable
        .findInConfiguration(configuration)
        .getOrElse(throw GradleException(s"failed to add dependency $toAdd to configuration $configuration"))
    
    dependency.verifyRequired(
      result,
      version, 
      isVersionExact
    )
    
    result

object DependencyRequirement:
  private val logger: Logger = LoggerFactory.getLogger(DependencyRequirement.getClass)
  