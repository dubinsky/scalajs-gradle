package org.podval.tools.build

trait SimpleDependencyMaker[F <: SimpleDependency[F]] extends DependencyMaker.Jvm:
  override def findable: F
  final override def dependency(scalaVersion: ScalaVersion): F = dependency
  final def dependency: F = findable
