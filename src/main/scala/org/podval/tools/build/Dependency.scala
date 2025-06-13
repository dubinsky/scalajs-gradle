package org.podval.tools.build

abstract class Dependency:
  def maker: DependencyMaker

  def artifactNameSuffix: String

  final def withVersion(version: PreVersion): DependencyWithVersion = DependencyWithVersion(
    dependency = this, 
    version = version
  )
