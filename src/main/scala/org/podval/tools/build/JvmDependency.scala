package org.podval.tools.build

import org.gradle.api.artifacts.Configuration
import scala.jdk.CollectionConverters.SetHasAsScala
import java.io.File

trait JvmDependency extends Dependency:
  def forBackend(backend: Option[Backend]): JvmDependency

  def backend: Backend

  def isVersionCompound: Boolean

  def isScalaVersion(scalaVersion: Option[Version]): Boolean
  
  def fromVersion(scalaVersion: Option[Version], version: Version.Pre): DependencyVersion

  def withVersion(scalaLibrary: ScalaLibrary, version: Version): DependencyVersion

  final override def classifier(version: Version): Option[String] = None

  final override def extension (version: Version): Option[String] = None

  final override def backendSuffix: Option[String] = backend.artifactSuffix

  final def require(version: Version = versionDefault): DependencyRequirement = DependencyRequirement(
    dependency = this,
    version = version
  )

  final def findInConfiguration(configuration: Configuration): Option[DependencyVersion] = configuration
    .getDependencies
    .asScala
    .toSet
    .map(Artifact.fromDependency)
    .flatMap(find)
    .headOption

  final def findInClasspath(classpath: Iterable[File]): Option[DependencyVersion] = classpath
    .map(Artifact.fromFile)
    .flatMap(find)
    .headOption

  private def find(artifact: Artifact): Option[DependencyVersion] = artifact
    .version
    .flatMap(Version.parse(isVersionCompound, _))
    .flatMap: (version: Version.Pre) =>
      val scalaVersion: Option[Version] = artifact.scalaVersion.map(Version(_))
      val extension: Option[String] = this.extension(version.version)
      val found: Boolean =
        isScalaVersion(scalaVersion) &&
        artifact.group.fold(true)(_ == group) &&
        (artifact.name == this.artifact) &&
        (artifact.backend == backend.artifactSuffix) &&
        (artifact.classifier == classifier(version.version)) &&
        ((artifact.extension == extension) || (extension.isEmpty && artifact.extension.contains("jar")))

      Option.when(found)(fromVersion(scalaVersion = scalaVersion, version = version))
