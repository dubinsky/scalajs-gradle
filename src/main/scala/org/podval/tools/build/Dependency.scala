package org.podval.tools.build

import org.gradle.api.artifacts.Configuration
import org.podval.tools.gradle.Dependencies
import org.podval.tools.util.Strings.prefix
import java.io.File

trait Dependency:
  override def toString: String = s"$group:$artifact:$versionDefault"

  def scalaBackend: ScalaBackend
  // Note: the flavour with the `scalaVersion` parameter is needed only to accommodate AirSpec.
  def isBackendSupported(backend: ScalaBackend, scalaVersion: ScalaVersion): Boolean = isBackendSupported(backend)
  def isBackendSupported(backend: ScalaBackend): Boolean
  def withBackend(backend: ScalaBackend): Dependency
  def group: String
  def artifact: String
  def versionDefault: Version
  // Note: `backend` and `scalaLibrary` parameter is needed only to accommodate specs2 -
  // so if the need goes away, this can be simplified ;)
  def versionDefaultFor(backend: ScalaBackend, scalaLibrary: ScalaLibrary): Version = versionDefault
  def isVersionCompound: Boolean = false
  def description: String
  def classifier(version: PreVersion): Option[String]
  def extension(version: PreVersion): Option[String]
  def withScalaVersion(scalaLibrary: ScalaLibrary): Dependency.WithScalaVersion
  def forArtifact(artifactName: String): Option[Dependency.WithScalaVersion]

  final def required(
    version: PreVersion = versionDefault
  ): DependencyRequirement = DependencyRequirement(
    this,
    version
  )

  final def dependencyNotation(
    backend: ScalaBackend,
    scalaLibrary: ScalaLibrary,
    version: Option[PreVersion]
  ): String = this
    .withScalaVersion(scalaLibrary)
    .withVersion(version.getOrElse(versionDefaultFor(backend, scalaLibrary)))
    .dependencyNotation

  final def findInConfiguration(configuration: Configuration): Option[Dependency.WithVersion] =
    Dependencies.forConfiguration(configuration, find)

  final def findInClasspath(classpath: Iterable[File]): Option[Dependency.WithVersion] =
    Dependencies.forClasspath(classpath, find)

  private def find(dependencyData: Dependencies.DependencyData): Option[Dependency.WithVersion] =
    dependencyData.version.flatMap(PreVersion(_, isVersionCompound)).flatMap: version =>
      val extension: Option[String] = Dependency.this.extension(version)
      val matches: Boolean =
        (dependencyData.group.isEmpty || dependencyData.group.contains(group)) &&
        (dependencyData.classifier == classifier(version)) &&
        ((dependencyData.extension == extension) || (extension.isEmpty && dependencyData.extension.contains("jar")))
      if !matches
      then None
      else forArtifact(dependencyData.artifactName).map(_.withVersion(version))

object Dependency:
  abstract class WithScalaVersion:
    override def toString: String = s"${dependency.group}:$artifactName"
    final def artifactName: String = s"${dependency.artifact}$artifactNameSuffix"
    final def withVersion(version: PreVersion): Dependency.WithVersion = Dependency.WithVersion(this, version)
    def dependency: Dependency
    def artifactNameSuffix: String

  final class WithVersion(
    withScalaVersion: Dependency.WithScalaVersion,
    val version: PreVersion
  ):
    override def toString: String = dependencyNotation

    private def dependency: Dependency = withScalaVersion.dependency

    def dependencyNotation: String =
      s"${dependency.group}:$artifactName:$version${prefix(":", classifier)}${prefix("@", extension)}"

    def fileName: String =
      s"$artifactName-$version${prefix("-", classifier)}.${extension.getOrElse("jar")}"

    private def artifactName: String = withScalaVersion.artifactName
    private def classifier: Option[String] = dependency.classifier(version)
    private def extension : Option[String] = dependency.extension (version)
  
