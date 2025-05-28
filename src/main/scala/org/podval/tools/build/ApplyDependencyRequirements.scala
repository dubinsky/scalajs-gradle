package org.podval.tools.build

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.podval.tools.build.{DependencyRequirement, ScalaVersion}

// Arrays are used all the way to here for Scala 2.12 compatibility :(
final class ApplyDependencyRequirements(
  dependencyRequirements: Array[DependencyRequirement],
  scalaVersion: ScalaVersion,
  configurationName: String
):
  def apply(project: Project): Unit = if dependencyRequirements.length > 0 then
    val configuration: Configuration = project.getConfigurations.getByName(configurationName)
    dependencyRequirements.map(_.applyToConfiguration(
      project = project,
      scalaVersion = scalaVersion,
      configuration = configuration
    ))
