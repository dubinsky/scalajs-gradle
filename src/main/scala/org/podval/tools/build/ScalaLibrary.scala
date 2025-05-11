package org.podval.tools.build

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import java.io.File

final class ScalaLibrary private(
  val scala3: Option[Dependency.WithVersion],
  val scala2: Option[Dependency.WithVersion]
):
  def toPlatform(backendKind: ScalaBackendKind): ScalaPlatform = ScalaPlatform(
    scalaVersion = scala3.getOrElse(scala2.get).version,
    backendKind
  )

  override def toString: String = s"ScalaLibrary(scala3=${scala3.map(_.version)}, scala2=${scala2.map(_.version)})"

  def verify(
    configurationName: String,
    other: ScalaLibrary
  ): Unit =
    require(
      other.scala3.nonEmpty == scala3.nonEmpty,
      s"Scala 3 presence changed from ${scala3.nonEmpty} to ${other.scala3.nonEmpty} in configuration '$configurationName'."
    )
    if scala3.nonEmpty
    then require(
      other.scala3.get.version == scala3.get.version,
      s"Scala 3 version changed from ${scala3.get.version} to ${other.scala3.get.version} in configuration '$configurationName'."
    )
    else require(
      other.scala2.get.version == scala2.get.version,
      s"Scala 2 version changed from ${scala2.get.version} to ${other.scala2.get.version} in configuration '$configurationName'."
    )

object ScalaLibrary:
  def getFromConfiguration(
    project: Project,
    configurationName: String
  ): ScalaLibrary = ScalaLibrary(
    source = s"in configuration '$configurationName'",
    mustHaveScala2 = false,
    scala3 = ScalaVersion.Scala3.scalaLibraryDependency.findInConfiguration(project, configurationName),
    scala2 = ScalaVersion.Scala2.scalaLibraryDependency.findInConfiguration(project, configurationName)
  )

  def getFromClasspath(classPath: Iterable[File]): ScalaLibrary = ScalaLibrary(
    source = "on classpath " + classPath.mkString(", "),
    mustHaveScala2 = true,
    scala3 = ScalaVersion.Scala3.scalaLibraryDependency.findInClassPath(classPath),
    scala2 = ScalaVersion.Scala2.scalaLibraryDependency.findInClassPath(classPath)
  )

  private def apply(
    source: String,
    mustHaveScala2: Boolean,
    scala3: Option[Dependency.WithVersion],
    scala2: Option[Dependency.WithVersion]
  ): ScalaLibrary =
    require(scala3.nonEmpty || scala2.nonEmpty, s"No Scala library $source")
    if mustHaveScala2 then require(scala2.nonEmpty, s"No Scala 2 library $source")

    new ScalaLibrary(
      scala3, 
      scala2
    )
    