package org.podval.tools.build

import org.gradle.api.GradleException
import org.podval.tools.jvm.JvmBackend
import org.podval.tools.util.Strings

trait ScalaDependency extends Dependency:
  def isPublishedFor(scalaVersion: ScalaVersion): Boolean = true
  def isScalaVersionFull: Boolean = false

  final override def classifier(version: PreVersion): Option[String] = None
  final override def extension (version: PreVersion): Option[String] = None

  final override def forArtifact(artifactName: String): Option[ScalaDependency.WithScalaVersion] =
    val (artifactAndBackend: String, scalaVersionOpt: Option[String]) = Strings.split(artifactName, '_')
    val (artifact: String, backendSuffixOpt: Option[String]) = Strings.split(artifactAndBackend, '_')
    val matches: Boolean =
      (artifact == ScalaDependency.this.artifact) &&
      (backendSuffixOpt == scalaBackend.artifactSuffix)
    if !matches
    then None
    else scalaVersionOpt.map(Version(_).toScalaVersion).map(withScalaVersion)

  final override def withScalaVersion(scalaLibrary: ScalaLibrary): ScalaDependency.WithScalaVersion = withScalaVersion(
    scalaLibrary.scala3.filter(isPublishedFor)
      .orElse(Some(scalaLibrary.scala2).filter(isPublishedFor))
      .getOrElse(throw GradleException(s"Dependency $this is not published for $scalaLibrary."))
  )

  private def withScalaVersion(scalaVersion: ScalaVersion): ScalaDependency.WithScalaVersion =
    ScalaDependency.WithScalaVersion(dependency = this, scalaVersion)

  import ScalaDependency.Wrapper

  override def withBackend(backend: ScalaBackend): ScalaDependency =
    require(isBackendSupported(backend))
    if backend == scalaBackend then this else new Wrapper(this):
      final override def scalaBackend: ScalaBackend = backend

  final def jvm: ScalaDependency = withBackend(JvmBackend)
  
  final def scala3: ScalaDependency = new Wrapper(this):
    final override def isPublishedFor(scalaVersion: ScalaVersion): Boolean = scalaVersion.isScala3

  final def scala2: ScalaDependency = new Wrapper(this):
    final override def isPublishedFor(scalaVersion: ScalaVersion): Boolean = scalaVersion.isScala2

  final def scalaCompilerPlugin: ScalaDependency = (new Wrapper(this):
    final override def isScalaVersionFull: Boolean = true
  ).jvm

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
    override def isPublishedFor(scalaVersion: ScalaVersion): Boolean = delegate.isPublishedFor(scalaVersion)
    override def isScalaVersionFull: Boolean = delegate.isScalaVersionFull

  final class WithScalaVersion(
    override val dependency: ScalaDependency,
    scalaVersion: ScalaVersion
  ) extends Dependency.WithScalaVersion:
    override def artifactNameSuffix: String = dependency.scalaBackend.artifactNameSuffix(versionSuffix)

    def versionSuffix: Version =
      if dependency.isScalaVersionFull
      then scalaVersion.version
      else scalaVersion.binaryVersion.versionSuffix
