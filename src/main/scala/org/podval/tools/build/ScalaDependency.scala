package org.podval.tools.build

import org.gradle.api.GradleException
import org.podval.tools.jvm.JvmBackend

final case class ScalaDependency(
  override val backend: Backend,
  override val name: String,
  override val group: String,
  override val versionDefault: Version,
  override val artifact: String,
  override val isVersionCompound: Boolean = false,
  isJvm: Boolean = false,
  isPublishedForScala3: Boolean = true,
  isPublishedForScala2: Boolean = true,
  isScalaVersionFull: Boolean = false
) extends JvmDependency:
  def scala3: ScalaDependency = copy(isPublishedForScala2 = false)
  def scala2: ScalaDependency = copy(isPublishedForScala3 = false)
  def jvm: ScalaDependency = copy(isJvm = true, backend = JvmBackend)
  def scalaCompilerPlugin: ScalaDependency = copy(isScalaVersionFull = true).jvm
  def versionCompound: ScalaDependency = copy(isVersionCompound = true)

  override def forBackend(backend: Option[Backend]): ScalaDependency = backend match
    case None => this
    case Some(backend) =>
      if isJvm
      then this
      else this.copy(backend = backend)

  override def isScalaVersion(scalaVersion: Option[Version]): Boolean =
    scalaVersion.isDefined  // TODO check that it is long enough if isScalaVersionFull

  override def fromVersion(
    scalaVersion: Option[Version],
    version: Version.Pre
  ): DependencyVersion = withVersion(
    scalaVersion = ScalaVersion(scalaVersion.get),
    version = version
  )

  override def withVersion(scalaLibrary: ScalaLibrary, version: Version): DependencyVersion = withVersion(
    scalaVersion = scalaLibrary
      .scalaVersion(isPublishedForScala3, isPublishedForScala2)
      .getOrElse(throw GradleException(s"Dependency $this is not published for $scalaLibrary.")),
    version = Version.compose(
      isVersionCompound,
      scalaVersion = scalaLibrary.scalaVersion,
      version = version
    )
  )

  private def withVersion(
    scalaVersion: ScalaVersion,
    version: Version.Pre
  ): DependencyVersion = withVersion(
    version = version,
    scalaVersion = Some:
      if isScalaVersionFull
      then scalaVersion.version
      else scalaVersion.binaryVersion.prefix
  )
