package org.podval.tools.build

trait DependencyMaker:
  override def toString: String = s"$group:$artifact"

  final def required(
    version: PreVersion = versionDefault
  ): DependencyRequirement = DependencyRequirement(
    this,
    version
  )

  def group: String

  def artifact: String

  def versionDefault: Version

  def description: String

  def classifier(version: PreVersion): Option[String]

  def extension(version: PreVersion): Option[String]

  def findable: DependencyFindable[?]

  def dependency(scalaVersion: ScalaVersion): Dependency

  def scalaBackend: ScalaBackend

  def isVersionCompound: Boolean = false

  def useExactVersionInVerifyRequired: Boolean = false
