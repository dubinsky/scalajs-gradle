package org.podval.tools.build

import org.podval.tools.jvm.JvmBackend

trait SimpleDependencyMaker[F <: SimpleDependency[F]] extends DependencyMaker:
  override def findable: F

  final override def scalaBackend: JvmBackend.type = JvmBackend
  final override def dependency(scalaVersion: ScalaVersion): F = dependency
  final def dependency: F = findable

object SimpleDependencyMaker:
  abstract class Delegating[F <: SimpleDependency[F]](delegate: SimpleDependencyMaker[F])
    extends DependencyMaker.Delegating(delegate) with SimpleDependencyMaker[F]:
    
    override def findable: F = delegate.findable
