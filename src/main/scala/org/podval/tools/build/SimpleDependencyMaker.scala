package org.podval.tools.build

import org.podval.tools.backend.jvm.JvmBackend

trait SimpleDependencyMaker[F <: SimpleDependency[F]] extends DependencyMaker:
  override def findable: F

  final override def scalaBackend: JvmBackend.type = JvmBackend
  
  final override def dependency(scalaVersion: ScalaVersion): F = findable
  
  final def dependency: F = findable
