package org.podval.tools.build

import org.podval.tools.jvm.JvmBackend

trait DependencyMaker:
  override def toString: String = s"$group:$artifact"

  def scalaBackend: ScalaBackend
  def group: String
  def artifact: String
  def versionDefault: Version // TODO de-emphasize in favour of versionDefaultFor()
  // Note: backend and scalaVersion parameter is needed only to accommodate specs2
  def versionDefaultFor(backend: ScalaBackend, scalaVersion: ScalaVersion): Version = versionDefault
  def description: String
  def classifier(version: PreVersion): Option[String]
  def extension(version: PreVersion): Option[String]
  def findable: DependencyFindable[?]
  def dependency(scalaVersion: ScalaVersion): Dependency
  def isVersionCompound: Boolean = false
  def isDependencyRequirementVersionExact: Boolean = false
  
  final def required(
    version: PreVersion = versionDefault
  ): DependencyRequirement = DependencyRequirement(
    this,
    version
  )

object DependencyMaker:
  trait Jvm extends DependencyMaker:
    final override def scalaBackend: JvmBackend.type = JvmBackend

  trait IsVersionCompound extends DependencyMaker:
    final override def isVersionCompound: Boolean = true
