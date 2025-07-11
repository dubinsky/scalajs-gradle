package org.podval.tools.build

import org.podval.tools.jvm.JvmBackend

trait DependencyMaker:
  override def toString: String = s"$group:$artifact"

  def scalaBackend: ScalaBackend = JvmBackend
  def group: String
  def artifact: String
  def versionDefault: Version // TODO de-emphasize in favour of versionDefaultFor()
  // Note: scalaVersion parameter is needed only to accommodate specs2
  def versionDefaultFor(scalaVersion: ScalaVersion): Version = versionDefault
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
  abstract class Delegating(delegate: DependencyMaker) extends DependencyMaker:
    override def scalaBackend: ScalaBackend = delegate.scalaBackend
    override def group: String = delegate.group
    override def artifact: String = delegate.artifact
    override def versionDefault: Version = delegate.versionDefault
    override def versionDefaultFor(scalaVersion: ScalaVersion): Version = delegate.versionDefaultFor(scalaVersion)
    override def description: String = delegate.description
    override def classifier(version: PreVersion): Option[String] = delegate.classifier(version)
    override def extension(version: PreVersion): Option[String] = delegate.classifier(version)
    override def findable: DependencyFindable[?] = delegate.findable
    override def dependency(scalaVersion: ScalaVersion): Dependency = delegate.dependency(scalaVersion)
    override def isVersionCompound: Boolean = delegate.isVersionCompound
    override def isDependencyRequirementVersionExact: Boolean = delegate.isDependencyRequirementVersionExact
