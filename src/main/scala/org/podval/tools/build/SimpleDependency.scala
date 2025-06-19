package org.podval.tools.build

abstract class SimpleDependency[F <: SimpleDependency[F]] extends Dependency with DependencyFindable[F]:
  self: F =>

  final override def artifactNameSuffix: String = ""

  final override protected def dependencyForArtifactName(artifactName: String): Option[F] =
    if artifactName != maker.artifact then None else Some(this)
