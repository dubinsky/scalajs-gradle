package org.podval.tools.build

import org.gradle.api.GradleException
import org.podval.tools.jvm.JvmBackend

enum ScalaPublication derives CanEqual:
  case Both, Scala3, Scala2

enum DependencyPlatform derives CanEqual:
  case Backend, Jvm

final case class ScalaDependency(
  override val backend: Backend,
  override val name: String,
  override val group: String,
  override val versionDefault: Version,
  override val artifact: String,
  override val isVersionCompound: Boolean = false,
  publication: ScalaPublication = ScalaPublication.Both,
  platform: DependencyPlatform = DependencyPlatform.Backend,
  fullScalaVersion: Boolean = false
) extends JvmDependency:
  def scala3: ScalaDependency = copy(publication = ScalaPublication.Scala3)
  def scala2: ScalaDependency = copy(publication = ScalaPublication.Scala2)
  def jvm: ScalaDependency = copy(platform = DependencyPlatform.Jvm, backend = JvmBackend)
  def scalaCompilerPlugin: ScalaDependency = copy(fullScalaVersion = true).jvm
  def versionCompound: ScalaDependency = copy(isVersionCompound = true)

  private def publishedForScala3: Boolean = publication != ScalaPublication.Scala2
  private def publishedForScala2: Boolean = publication != ScalaPublication.Scala3

  override def forBackend(backend: Option[Backend]): ScalaDependency = backend match
    case None => this
    case Some(backend) =>
      if platform == DependencyPlatform.Jvm
      then this
      else this.copy(backend = backend)

  override def isScalaVersion(scalaVersion: Option[Version]): Boolean =
    scalaVersion.isDefined  // TODO check that it is long enough if fullScalaVersion

  override def fromVersion(
    scalaVersion: Option[Version],
    version: Version.Pre
  ): DependencyVersion = withVersion(
    scalaVersion = ScalaVersion(scalaVersion.get),
    version = version
  )

  override def withVersion(scalaLibrary: ScalaLibrary, version: Version): DependencyVersion = withVersion(
    scalaVersion = scalaLibrary
      .scalaVersion(publishedForScala3, publishedForScala2)
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
      if fullScalaVersion
      then scalaVersion.version
      else scalaVersion.crossVersion
  )
