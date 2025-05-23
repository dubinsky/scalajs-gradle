package org.podval.tools.build

import org.podval.tools.build.{DependencyRequirement, ScalaPlatform}

final class BackendDependencyRequirements(
  val implementation: Array[DependencyRequirement[ScalaPlatform]],
  val testRuntimeOnly: Array[DependencyRequirement[ScalaPlatform]],
  val scalaCompilerPlugins: Array[DependencyRequirement[ScalaPlatform]],
  val pluginDependencies: Array[DependencyRequirement[ScalaPlatform]]
)
