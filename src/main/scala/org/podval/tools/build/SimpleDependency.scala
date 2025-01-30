package org.podval.tools.build

abstract class SimpleDependency[F <: SimpleDependency[F]](
  group: String,
  artifact: String
) extends Dependency(
  group,
  artifact
) with FindableDependency[F]:
  self: F =>

  final override protected def artifactNameSuffix: String = ""

  final override protected def dependencyForArtifactName(artifactName: String): Option[F] =
    if artifactName != this.artifact then None else Some(this)
