package org.podval.tools.build

import org.gradle.api.GradleException
import org.podval.tools.platform.Strings

trait ScalaDependency extends Dependency:
  override def isJvm: Boolean = false
  def isPublishedForScala3: Boolean = true
  def isPublishedForScala2: Boolean = true
  def isScalaVersionFull: Boolean = false
  
  final override def classifier(version: Version): Option[String] = None
  final override def extension (version: Version): Option[String] = None

  final override def forArtifact(artifactName: String): Option[ScalaDependency.WithScalaVersion] =
    val (artifactAndBackend: String, scalaVersion: Option[String]) = Strings.split(artifactName, '_')
    val (artifact: String, artifactSuffix: Option[String]) = Strings.split(artifactAndBackend, '_')
    val matches: Boolean =
      (artifact == ScalaDependency.this.artifact) &&
      (artifactSuffix == scalaBackend(backendOverride = None).artifactSuffix)
    if !matches then None else scalaVersion.map(ScalaVersion(_)).map(withScalaVersion)

  final def artifactNameSuffix(
    backendOverride: Option[ScalaBackend],
    scalaVersion: ScalaVersion
  ): String =
    scalaBackend(backendOverride).artifactNameSuffix(scalaVersion.versionSuffix(isScalaVersionFull))

  final override def withScalaVersion(scalaLibrary: ScalaLibrary): ScalaDependency.WithScalaVersion = withScalaVersion(
    scalaLibrary.scala3.filter(_ => isPublishedForScala3)
      .orElse(Some(scalaLibrary.scala2).filter(_ => isPublishedForScala2))
      .getOrElse(throw GradleException(s"Dependency $this is not published for $scalaLibrary."))
  )

  private def withScalaVersion(scalaVersion: ScalaVersion): ScalaDependency.WithScalaVersion =
    ScalaDependency.WithScalaVersion(dependency = this, scalaVersion)

  import ScalaDependency.Wrapper

  final def jvm: ScalaDependency = new Wrapper(this):
    final override def isJvm: Boolean = true
  
  final def scala3: ScalaDependency = new Wrapper(this):
    final override def isPublishedForScala2: Boolean = false

  final def scala2: ScalaDependency = new Wrapper(this):
    final override def isPublishedForScala3: Boolean = false

  final def scalaCompilerPlugin: ScalaDependency = new Wrapper(this):
    final override def isScalaVersionFull: Boolean = true
    final override def isJvm: Boolean = true

  final def withVersionCompound: ScalaDependency = new Wrapper(this):
    final override def isVersionCompound: Boolean = true

object ScalaDependency:
  def apply(
    backend: ScalaBackend,
    groupId: String,
    artifactId: String,
    version: Version,
    what: String
  ): ScalaDependency = new ScalaDependency:
    override def scalaBackend: ScalaBackend = backend
    override def isBackendSupported(backend: ScalaBackend): Boolean = true
    override def group: String = groupId
    override def artifact: String = artifactId
    override def versionDefault: Version = version
    override def description: String = s"${backend.name} $what"

  private abstract class Wrapper(delegate: ScalaDependency) extends ScalaDependency:
    override def scalaBackend: ScalaBackend = delegate.scalaBackend
    override def isBackendSupported(backend: ScalaBackend): Boolean = delegate.isBackendSupported(backend)
    override def isBackendSupported(backend: ScalaBackend, scalaVersion: ScalaVersion): Boolean = delegate.isBackendSupported(backend, scalaVersion)
    override def group: String = delegate.group
    override def artifact: String = delegate.artifact
    override def versionDefault: Version = delegate.versionDefault
    override def versionDefaultFor(backend: ScalaBackend, scalaLibrary: ScalaLibrary): Version = delegate.versionDefaultFor(backend, scalaLibrary)
    override def description: String = delegate.description
    override def isVersionCompound: Boolean = delegate.isVersionCompound
    override def isJvm: Boolean = delegate.isJvm
    override def isPublishedForScala3: Boolean = delegate.isPublishedForScala3
    override def isPublishedForScala2: Boolean = delegate.isPublishedForScala2
    override def isScalaVersionFull: Boolean = delegate.isScalaVersionFull

  final class WithScalaVersion(
    override val dependency: ScalaDependency,
    scalaVersion: ScalaVersion
  ) extends Dependency.WithScalaVersion:
    override def artifactNameSuffix(backendOverride: Option[ScalaBackend]): String = dependency
      .artifactNameSuffix(backendOverride, scalaVersion)
