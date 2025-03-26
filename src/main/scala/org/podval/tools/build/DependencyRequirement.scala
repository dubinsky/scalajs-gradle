package org.podval.tools.build

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
    val result: Dependency.WithVersion = findable.findInConfiguration(project, configurationName).getOrElse:
      val toAdd: Dependency.WithVersion = dependency.withVersion(version)
      DependencyRequirement.logger.info(s"Adding dependency $toAdd to the $configurationName $reason")
      Gradle
        .getConfiguration(project, configurationName)
        .getDependencies
        .add(project.getDependencies.create(toAdd.dependencyNotation))
      findable
        .findInConfiguration(project, configurationName)
        .getOrElse(throw GradleException(s"failed to add dependency $toAdd to configuration $configurationName"))
    
    dependency.verifyRequired(
      result,
      version, 
      isVersionExact
    )
    
    result

object DependencyRequirement:
  private val logger: Logger = LoggerFactory.getLogger(DependencyRequirement.getClass)
  