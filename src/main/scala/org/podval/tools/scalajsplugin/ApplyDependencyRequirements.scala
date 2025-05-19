package org.podval.tools.scalajsplugin

import org.gradle.api.Project
import org.podval.tools.build.{DependencyRequirement, ScalaPlatform}

// Arrays are used all the way to here for Scala 2.12 compatibility :(
final class ApplyDependencyRequirements(
  dependencyRequirements: Array[DependencyRequirement[ScalaPlatform]],
  scalaPlatform: ScalaPlatform,
  configurationName: String
):
  def apply(
    project: Project,
  ): Unit =
    dependencyRequirements.map(_.applyToConfiguration(
      project,
      project.getConfigurations.getByName(configurationName),
      scalaPlatform
    ))
