package org.podval.tools.build

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.podval.tools.gradle.{Artifact, Configurations, GradleClasspath}
import scala.jdk.CollectionConverters.IterableHasAsScala
import java.io.File

sealed abstract class ScalaLibrary(val scalaVersion: ScalaVersion):
  final def scalaBinaryVersionSuffix: Version = scalaVersion.binaryVersionSuffix
  def scala2BinaryVersionSuffix: Version
  def isScala3: Boolean

  def scalaVersion(
    isPublishedForScala3: Boolean,
    isPublishedForScala2: Boolean
  ): Option[ScalaVersion]

  final def verify(project: Project): Unit =
    val runtimeClasspath: Configuration = Configurations.runtimeClasspath(project)
    val other: ScalaLibrary = ScalaLibrary.fromClasspath(
      project,
      source = s"'${runtimeClasspath.getName}'",
      classPath = runtimeClasspath.asScala
    )
    require(this.toString == other.toString, s"Scala library changed from $this to $other")

object ScalaLibrary:
  final class Scala2(scalaVersion: ScalaVersion /* TODO .Scala2*/) extends ScalaLibrary(scalaVersion):
    override def toString: String = s"Scala2Library($scalaVersion)"
    override def isScala3: Boolean = false
    override def scala2BinaryVersionSuffix: Version = scalaBinaryVersionSuffix

    override def scalaVersion(
      isPublishedForScala3: Boolean,
      isPublishedForScala2: Boolean
    ): Option[ScalaVersion] = Option.when(isPublishedForScala2)(scalaVersion)

  // For Scala 3, we *approximate* Scala 2;
  // this is safe, since the full Scala 2 version
  // matters only for dependencies `withScalaVersionFull`,
  // and ScalaLibrary constructed here is only used with `FrameworkDescriptor` dependencies,
  // which are not `withScalaVersionFull`.
  // Scala 2 version used by Scala 3 from 3.0.0 to 3.8.0 is 2.13;
  // starting with 3.8.0, the version is the same as that of the Scala 3, with the major of 3.

  sealed abstract class Scala3(scalaVersion: ScalaVersion/* TODO .Scala3*/) extends ScalaLibrary(scalaVersion):
    override def toString: String = s"Scala3Library($scalaVersion, $scala2Version)"
    def scala2Version: ScalaVersion /* TODO .Scala2*/
    final override def isScala3: Boolean = true
    final override def scala2BinaryVersionSuffix: Version = scala2Version.binaryVersionSuffix

    final override def scalaVersion(
      isPublishedForScala3: Boolean,
      isPublishedForScala2: Boolean
    ): Option[ScalaVersion] =
      Option.when(isPublishedForScala3)(scalaVersion)
        .orElse(Option.when(isPublishedForScala2)(scala2Version))

  final class Scala3LibraryCompiledWithScala2(
    scalaVersion: ScalaVersion,
    override val scala2Version: ScalaVersion
  ) extends Scala3(scalaVersion)

  final class Scala3LibraryCompiledWithScala3(scalaVersion: ScalaVersion /* TODO .Scala3*/) extends Scala3(scalaVersion):
    override def scala2Version: ScalaVersion = ScalaBinaryVersion.Scala3.scala2VersionDefault

  def fromImplementationConfiguration(project: Project): ScalaLibrary =
    val implementation: Configuration = Configurations.implementation(project)
    ScalaLibrary(
      project,
      source = s"in configuration '${implementation.getName}'",
      isFromClasspath = false,
      find = _.findInConfiguration(implementation)
    )

  def fromAmbientClasspath(project: Project): ScalaLibrary = fromClasspath(
    project,
    source = "ambient classpath",
    classPath = GradleClasspath.collect
  )

  private def fromClasspath(
    project: Project,
    source: String,
    classPath: Iterable[File]
  ): ScalaLibrary = ScalaLibrary(
    project,
    source = s"on $source: ${classPath.mkString(", ")}",
    isFromClasspath = true,
    find = _.findInClasspath(classPath)
  )

  def fromScalaVersion(scalaVersion: ScalaVersion): ScalaLibrary =
    if scalaVersion.isScala2
    then Scala2(scalaVersion)
    else
      if ScalaBinaryVersion.Scala3.libraryCompiledWithScala2(scalaVersion)
      then Scala3LibraryCompiledWithScala2(scalaVersion, ScalaBinaryVersion.Scala3.scala2VersionDefault)
      else Scala3LibraryCompiledWithScala3(scalaVersion)

  private def apply(
    project: Project,
    source: String,
    isFromClasspath: Boolean,
    find: JavaDependency => Option[Dependency.WithVersion]
  ): ScalaLibrary =
    val scala3: Option[Dependency.WithVersion] = find(ScalaBinaryVersion.Scala3   )
    // Note: this finds any Scala 2 library, even if it is 2.12, not 2.13 - and even if it is 3.8.0 :)
    val scala2: Option[Dependency.WithVersion] = find(ScalaBinaryVersion.Scala2_13)

    require(scala3.nonEmpty || scala2.nonEmpty, s"No Scala library $source.")
    if isFromClasspath then require(scala2.nonEmpty, s"No Scala 2 library $source.")
    if !isFromClasspath then require(scala3.isEmpty || scala2.isEmpty, s"Both Scala 3 and Scala 2 library present $source.")

    if scala2.isDefined then
      def toScalaVersion(withVersion: Option[Dependency.WithVersion]): ScalaVersion = ScalaVersion(withVersion.get.version.version)
      val scala2Version: ScalaVersion = toScalaVersion(scala2)
      if scala3.isEmpty
      then Scala2(scala2Version)
      else
        val scala3Version: ScalaVersion = toScalaVersion(scala3)
        if ScalaBinaryVersion.Scala3.libraryCompiledWithScala2(scala3Version)
        then Scala3LibraryCompiledWithScala2(scala3Version, scala2Version)
        else
          require(scala2Version == scala3Version)
          Scala3LibraryCompiledWithScala3(scala3Version)
    else
      // When constructing Scala 3 ScalaLibrary `fromImplementationConfiguration`,
      // we do not get Scala 2 library dependency - and we need it for dependencies `withScalaVersionFull`;
      // we can't resolve the `runtimeClasspath` configuration,
      // because we need to add dependencies to various configurations,
      // some of which feed into `runtimeClasspath`;
      // so we transitively resolve the Scala 3 library dependency
      // and build ScalaLibrary from the resulting classpath:
      fromClasspath(
        project,
        source = s"'${scala3.get}' resolved",
        classPath = Artifact.resolveTransitive(
          project,
          dependencyNotation = scala3.get.dependencyNotation(backendOverride = None),
          repository = None
        )
      )
