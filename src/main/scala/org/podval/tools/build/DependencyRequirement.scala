package org.podval.tools.build

import org.gradle.api.artifacts.Configuration
import org.gradle.api.{GradleException, Project}

abstract class DependencyRequirement(
  findable: Dependency.Findable,
  version: Version,
  reason: String,
  configurationName: String,
  isVersionExact: Boolean = false
):
  // Note: all applyToConfiguration() must be run first: once a applyToClassPath() runs,
  // configuration is no longer changeable.
  final def applyToConfiguration(project: Project): Dependency.WithVersion =
    val configuration: Configuration = Gradle.getConfiguration(project, configurationName)
    val result: Dependency.WithVersion = findable.findInConfiguration(configuration).getOrElse {
      val toAdd: Dependency.WithVersion = getDependency.withVersion(version)
      project.getLogger.info(s"Adding dependency $toAdd to the $configuration $reason", null, null, null)
      configuration
        .getDependencies
        .add(project.getDependencies.create(toAdd.dependencyNotation))
      findable
        .findInConfiguration(configuration)
        .getOrElse(throw GradleException(s"failed to add dependency $toAdd to configuration $configuration"))
    }
    verify(result, project)
    result

  protected def verify(found: Dependency.WithVersion, project: Project): Unit =
    if isVersionExact && found.version != version then project.getLogger.info(
      s"Found $found, but the project uses version $version", null, null, null
    )
  
  protected def getDependency: Dependency
