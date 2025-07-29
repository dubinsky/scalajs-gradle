package org.podval.tools.build

trait SimpleDependencyMaker[F <: SimpleDependency[F]] extends DependencyMaker:
  override def findable: F
  final override def dependency(scalaLibrary: ScalaLibrary): F = dependency
  final def dependency: F = findable
