package org.podval.tools.build

import org.podval.tools.jvm.JvmBackend

open class JavaDependency(
  final override val name: String,
  final override val group: String,
  final override val versionDefault: Version,
  final override val artifact: String,
  // to handle 'scala3-library_3'
  final val scalaVersion: Option[Version] = None
) extends JvmDependency:
  final override def forBackend(backend: Option[Backend]): JavaDependency = this
  final override def backend: JvmBackend.type = JvmBackend
  final override def isVersionCompound: Boolean = false

  final override def isScalaVersion(scalaVersion: Option[Version]): Boolean =
    scalaVersion == this.scalaVersion

  final override def fromVersion(
    scalaVersion: Option[Version],
    version: Version.Pre
  ): DependencyVersion =
    withVersion(version.nonCompound)

  final override def withVersion(scalaLibrary: ScalaLibrary, version: Version): DependencyVersion =
    withVersion(version)

  private def withVersion(version: Version): DependencyVersion = withVersion(
    version = version,
    scalaVersion = scalaVersion
  )
