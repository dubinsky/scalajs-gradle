package org.podval.tools.build

import org.gradle.api.artifacts.Configuration
import org.gradle.api.{GradleException, Project}

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
      project.getLogger.info(s"Adding dependency $toAdd to the $configuration $reason", null, null, null)
      configuration
        .getDependencies
        .add(project.getDependencies.create(toAdd.dependencyNotation))
      findable
        .findInConfiguration(configuration)
        .getOrElse(throw GradleException(s"failed to add dependency $toAdd to configuration $configuration"))
    
    dependency.verifyRequired(
      result,
      version, 
      isVersionExact,
      project
    )
    
    result
