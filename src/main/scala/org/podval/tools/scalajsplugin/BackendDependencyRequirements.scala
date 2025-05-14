package org.podval.tools.scalajsplugin

import org.podval.tools.build.{DependencyRequirement, ScalaPlatform}

final class BackendDependencyRequirements(
  val implementation: Seq[DependencyRequirement[ScalaPlatform]],
  val testImplementation: Seq[DependencyRequirement[ScalaPlatform]],
  val scalaCompilerPlugins: Seq[DependencyRequirement[ScalaPlatform]],
  val pluginDependencies: Seq[DependencyRequirement[ScalaPlatform]]
)
