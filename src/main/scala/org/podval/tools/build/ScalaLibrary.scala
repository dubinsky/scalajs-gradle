package org.podval.tools.build

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.podval.tools.gradle.{Artifact, Configurations, GradleClasspath}
import scala.jdk.CollectionConverters.IterableHasAsScala
import java.io.File

sealed trait ScalaLibrary:
  def scalaVersion: ScalaVersion.Known

  final def scalaBinaryVersionPrefix: Version = scalaVersion.binaryVersion.prefix

  def scala2BinaryVersionPrefix: Version

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
// TODO with 3.7.4 and 3.8.0, this fails on 3.7.4: Scala 2 version changes...
//    require(this.toString == other.toString, s"Scala library changed from $this to $other")

object ScalaLibrary:
  private sealed trait Scala3 extends ScalaLibrary:
    def scala2Version: ScalaVersion

    final override def scala2BinaryVersionPrefix: Version = scala2Version.binaryVersion.prefix

    final override def scalaVersion(
      isPublishedForScala3: Boolean,
      isPublishedForScala2: Boolean
    ): Option[ScalaVersion] =
      Option.when(isPublishedForScala3)(scalaVersion)
        .orElse(Option.when(isPublishedForScala2)(scala2Version))

  private final class Scala3WithScala3Library(
    override val scalaVersion: ScalaVersion.Known
  ) extends Scala3:
    require(scalaVersion.binaryVersion == ScalaBinaryVersion.Scala3WithScala3Library)
    require(scala2Version.binaryVersion == ScalaBinaryVersion.Scala2_13)

    override def toString: String = s"Scala3WithScala3Library($scalaVersion)"
    override def scala2Version: ScalaVersion = ScalaVersion.Unknow2_13

  private final class Scala3WithScala2Library(
    override val scalaVersion: ScalaVersion.Known,
    override val scala2Version: ScalaVersion
  ) extends Scala3:
    require(scalaVersion.binaryVersion == ScalaBinaryVersion.Scala3WithScala2Library)
    require(scala2Version.binaryVersion.isInstanceOf[ScalaBinaryVersion.Scala2])

    override def toString: String = s"Scala3WithScala2Library($scalaVersion, $scala2Version)"

  private final class Scala2(
    override val scalaVersion: ScalaVersion.Known
  ) extends ScalaLibrary:
    require(scalaVersion.binaryVersion.isInstanceOf[ScalaBinaryVersion.Scala2])

    override def toString: String = s"Scala2($scalaVersion)"
    override def scala2BinaryVersionPrefix: Version = scalaBinaryVersionPrefix

    override def scalaVersion(
      isPublishedForScala3: Boolean,
      isPublishedForScala2: Boolean
    ): Option[ScalaVersion] = Option.when(isPublishedForScala2)(scalaVersion)

  def fromScalaVersion(scala: ScalaVersion.Known): ScalaLibrary = scala match
    case scala3@ScalaVersion.Known(ScalaBinaryVersion.Scala3WithScala3Library, _) =>
      Scala3WithScala3Library(scala3)
    case scala3@ScalaVersion.Known(ScalaBinaryVersion.Scala3WithScala2Library, _) =>
      Scala3WithScala2Library(scala3, ScalaVersion.Unknow2_13)
    case scala2@ScalaVersion.Known(_: ScalaBinaryVersion.Scala2, _) =>
      Scala2(scala2)
  
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

  private def apply(
    project: Project,
    source: String,
    isFromClasspath: Boolean,
    find: JavaDependency => Option[Dependency.WithVersion]
  ): ScalaLibrary =
    def toScalaVersion(dependencyWithVersion: Dependency.WithVersion): ScalaVersion.Known =
      ScalaVersion(dependencyWithVersion.version.version)

    // Note: version does not affect find(); for example find(Scala2_13)
    // finds any Scala 2 library, even if it is 2.12, not 2.13 - and even if it is 3.8.0 :)
    (
      find(ScalaBinaryVersion.Scala3WithScala3Library),
      find(ScalaBinaryVersion.Scala2_13)
    ) match

      case (None, None) =>
        throw IllegalArgumentException(s"No Scala library $source.")

      case (None, Some(scala2)) =>
        Scala2(toScalaVersion(scala2))

      case (Some(scala3), Some(scala2)) =>
        require(isFromClasspath, s"Both Scala 3 and Scala 2 library present $source.")

        val scala2Version: ScalaVersion.Known = toScalaVersion(scala2)
        toScalaVersion(scala3) match
          case scala3@ScalaVersion.Known(ScalaBinaryVersion.Scala3WithScala3Library, _) =>
            require(scala2Version == scala3)
            Scala3WithScala3Library(scala3)
          case scala3@ScalaVersion.Known(ScalaBinaryVersion.Scala3WithScala2Library, _) =>
            Scala3WithScala2Library(scala3, scala2Version)
          case scala2@ScalaVersion.Known(_: ScalaBinaryVersion.Scala2, _) =>
            throw IllegalArgumentException(s"$scala2 is not a Scala 3 version")

      case (Some(scala3), None) =>
        require(!isFromClasspath, s"No Scala 2 library $source.")
        // When constructing Scala 3 ScalaLibrary `fromImplementationConfiguration`,
        // we do not get Scala 2 library dependency - and we need it for dependencies with `isScalaVersionFull`;
        // we can't resolve the `runtimeClasspath` configuration,
        // because we need to add dependencies to various configurations,
        // some of which feed into `runtimeClasspath`;
        // so we transitively resolve the Scala 3 library dependency
        // and build ScalaLibrary from the resulting classpath:
        fromClasspath(
          project,
          source = s"'$scala3' resolved",
          classPath = Artifact.resolveTransitive(
            project,
            dependencyNotation = scala3.dependencyNotation(),
            repository = None
          )
        )
