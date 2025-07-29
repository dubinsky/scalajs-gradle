package org.podval.tools.build

import org.podval.tools.jvm.JvmBackend

trait DependencyMaker:
  override def toString: String = s"$group:$artifact"

  def scalaBackend: ScalaBackend = JvmBackend
  def group: String
  def artifact: String
  def versionDefault: Version // TODO de-emphasize in favour of versionDefaultFor()
  // Note: `backend` and `scalaLibrary` parameter is needed only to accommodate specs2 -
  // so if the need goes away, this can be simplified ;)
  def versionDefaultFor(backend: ScalaBackend, scalaLibrary: ScalaLibrary): Version = versionDefault
  def description: String
  def classifier(version: PreVersion): Option[String]
  def extension(version: PreVersion): Option[String]
  def findable: DependencyFindable[?]
  def dependency(scalaLibrary: ScalaLibrary): Dependency
  def isVersionCompound: Boolean = false
  def isDependencyRequirementVersionExact: Boolean = false
  
  final def required(
    version: PreVersion = versionDefault
  ): DependencyRequirement = DependencyRequirement(
    this,
    version
  )
