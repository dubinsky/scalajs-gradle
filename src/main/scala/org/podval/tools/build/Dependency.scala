package org.podval.tools.build

import org.gradle.api.artifacts.Configuration
import org.podval.tools.gradle.Dependencies
import org.podval.tools.jvm.JvmBackend
import org.podval.tools.platform.Strings.prefix
import java.io.File

trait Dependency:
  override def toString: String = s"$group:$artifact:$versionDefault" // TODO add backend suffix

  def isJvm: Boolean
  def scalaBackend: ScalaBackend
  // Note: the flavour with the `scalaVersion` parameter is needed only to accommodate AirSpec.
  def isBackendSupported(backend: ScalaBackend, scalaVersion: ScalaVersion): Boolean = isBackendSupported(backend)
  def isBackendSupported(backend: ScalaBackend): Boolean
  def group: String
  def artifact: String
  def versionDefault: Version
  // Note: `backend` and `scalaLibrary` parameter is needed only to accommodate specs2 -
  // so if the need goes away, this can be simplified ;)
  def versionDefaultFor(backend: ScalaBackend, scalaLibrary: ScalaLibrary): Version = versionDefault
  def isVersionCompound: Boolean = false
  def description: String
  def classifier(version: Version): Option[String]
  def extension (version: Version): Option[String]
  def withScalaVersion(scalaLibrary: ScalaLibrary): Dependency.WithScalaVersion
  def forArtifact(artifactName: String): Option[Dependency.WithScalaVersion]

  final def scalaBackend(backendOverride: Option[ScalaBackend]): ScalaBackend =
    if isJvm
    then JvmBackend
    else backendOverride.getOrElse(scalaBackend)

  final def dependencyNotation(artifactNameSuffix: String, version: Version.Pre): String =
    s"$group:$artifact$artifactNameSuffix:$version${prefix(":", classifier(version.version))}${prefix("@", extension(version.version))}"

  // TODO why is this 'this.' necessary?!
  final def fileName(artifactNameSuffix: String, version: Version.Pre): String =
    s"$artifact$artifactNameSuffix-$version${prefix("-", classifier(version.version))}.${this.extension(version.version).getOrElse("jar")}"

  final def required(version: Version): DependencyRequirement = DependencyRequirement(this, versionOverride = Some(version))
  final def required()                : DependencyRequirement = DependencyRequirement(this, versionOverride = None)

  final def dependencyNotation(
    backendOverride: Option[ScalaBackend],
    versionOverride: Option[Version],
    scalaLibrary: ScalaLibrary
  ): String = this
    .withScalaVersion(scalaLibrary)
    .withVersion(Version.compose(
      isVersionCompound,
      scalaVersion = scalaLibrary.scalaVersion,
      version = versionOverride.getOrElse(versionDefaultFor(scalaBackend(backendOverride), scalaLibrary))
    ))
    .dependencyNotation(backendOverride)

  final def findInConfiguration(configuration: Configuration): Option[Dependency.WithVersion] =
    Dependencies.forConfiguration(configuration, find)

  final def findInClasspath(classpath: Iterable[File]): Option[Dependency.WithVersion] =
    Dependencies.forClasspath(classpath, find)

  private def find(dependencyData: Dependencies.DependencyData): Option[Dependency.WithVersion] =
    dependencyData.version.flatMap(Version.parse(isVersionCompound, _)).flatMap: (version: Version.Pre) =>
      val extension: Option[String] = Dependency.this.extension(version.version)
      val matches: Boolean =
        (dependencyData.group.isEmpty || dependencyData.group.contains(group)) &&
        (dependencyData.classifier == classifier(version.version)) &&
        ((dependencyData.extension == extension) || (extension.isEmpty && dependencyData.extension.contains("jar")))
      if !matches
      then None
      else forArtifact(dependencyData.artifactName).map(_.withVersion(version))

object Dependency:
  abstract class WithScalaVersion:
    override def toString: String = s"${dependency.group}:${dependency.artifact}${artifactNameSuffix(backendOverride = None)}"

    def dependency: Dependency

    final def withVersion(version: Version.Pre): Dependency.WithVersion = Dependency.WithVersion(this, version)

    def artifactNameSuffix(backendOverride: Option[ScalaBackend]): String

    final def dependencyNotation(version: Version.Pre, backendOverride: Option[ScalaBackend]): String = dependency
      .dependencyNotation(artifactNameSuffix(backendOverride), version)

    final def fileName(version: Version.Pre): String = dependency
      .fileName(artifactNameSuffix(backendOverride = None), version)

  final class WithVersion(
    withScalaVersion: Dependency.WithScalaVersion,
    val version: Version.Pre
  ):
    override def toString: String = dependencyNotation(backendOverride = None)

    def dependencyNotation(backendOverride: Option[ScalaBackend]): String = withScalaVersion
      .dependencyNotation(version, backendOverride)

    def fileName: String = withScalaVersion
      .fileName(version)
