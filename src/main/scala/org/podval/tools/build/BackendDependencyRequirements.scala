package org.podval.tools.build

import org.podval.tools.build.DependencyRequirement

final class BackendDependencyRequirements(
  val implementation          : Array[DependencyRequirement],
  val testRuntimeOnly         : Array[DependencyRequirement],
  val scalaCompilerPlugins    : Array[DependencyRequirement],
  val testScalaCompilerPlugins: Array[DependencyRequirement],
  val pluginDependencies      : Array[DependencyRequirement]
)
