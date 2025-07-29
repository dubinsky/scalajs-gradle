package org.podval.tools.build

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.podval.tools.gradle.{Artifact, Configurations, GradleClasspath}
import scala.jdk.CollectionConverters.IterableHasAsScala
import java.io.File

final class ScalaLibrary private(
  val scala3: Option[ScalaVersion],
  val scala2: ScalaVersion
):
  override def toString: String = s"ScalaLibrary(scala3=$scala3, scala2=$scala2)"

  def isScala3: Boolean = scala3.isDefined

  def scalaVersion: ScalaVersion = scala3.getOrElse(scala2)

  def verify(project: Project): Unit =
    val runtimeClasspath: Configuration = Configurations.runtimeClasspath(project)
    val other: ScalaLibrary = ScalaLibrary.fromClasspath(
      project,
      source = s"'${runtimeClasspath.getName}'",
      classPath = runtimeClasspath.asScala
    )
    require(
      other.scala3.nonEmpty == scala3.nonEmpty,
      s"Scala 3 presence changed from ${scala3.nonEmpty} to ${other.scala3.nonEmpty}."
    )
    if scala3.nonEmpty
    then require(
      other.scala3.get == scala3.get,
      s"Scala 3 version changed from ${scala3.get} to ${other.scala3.get}."
    )
    else require(
      other.scala2 == scala2,
      s"Scala 2 version changed from $scala2 to $other.scala2."
    )

object ScalaLibrary:
  def fromScalaVersion(scalaVersion: ScalaVersion): ScalaLibrary =
    val isScala3: Boolean = scalaVersion.isScala3
    new ScalaLibrary(
      scala3 = if isScala3 then Some(scalaVersion) else None,
      scala2 = if !isScala3 then scalaVersion else
        // For Scala 3, we *approximate* Scala 2;
        // this is safe, since the full Scala 2 version
        // matters only for dependencies `withScalaVersionFull`,
        // and ScalaLibrary constructed here is only used with `FrameworkDescriptor` dependencies,
        // which are not `withScalaVersionFull`.
        // Scala 2 version used by Scala 3 from 3.0.0 to the current is 2.13:
        ScalaBinaryVersion.Scala2_13.scalaVersionDefault
    )

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
    find: JavaDependency => Option[Dependency#WithVersion]
  ): ScalaLibrary =
    val scala3: Option[Dependency#WithVersion] = find(ScalaBinaryVersion.Scala3   .dependency)
    // Note: this finds any Scala 2 library, even if it is 2.12, not 2.13:
    val scala2: Option[Dependency#WithVersion] = find(ScalaBinaryVersion.Scala2_13.dependency)

    require(scala3.nonEmpty || scala2.nonEmpty, s"No Scala library $source.")
    if isFromClasspath then require(scala2.nonEmpty, s"No Scala 2 library $source.")
    if !isFromClasspath then require(scala3.isEmpty || scala2.isEmpty, s"Both Scala 3 and Scala 2 library present $source.")

    if scala2.isDefined then
      new ScalaLibrary(
        scala3.map(_.version.simple.toScalaVersion),
        scala2.get  .version.simple.toScalaVersion
      )
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
        classPath = Artifact.resolveTransitive(project, scala3.get.dependencyNotation, None)
      )
