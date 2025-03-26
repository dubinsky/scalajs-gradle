package org.podval.tools.build

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import java.io.File

final class ScalaLibrary(
  val scala3: Option[Dependency.WithVersion],
  val scala2: Option[Dependency.WithVersion]
):
  require(scala3.nonEmpty || scala2.nonEmpty, "No Scala library!")
  
  def toPlatform(backend: ScalaBackend): ScalaPlatform = ScalaPlatform(
    scalaVersion = scala3.getOrElse(scala2.get).version,
    backend
  )

  override def toString: String = s"ScalaLibrary(scala3=${scala3.map(_.version)}, scala2=${scala2.map(_.version)})"

  def verify(other: ScalaLibrary): Unit =
    require(
      other.scala3.nonEmpty == scala3.nonEmpty,
      s"Scala 3 presence changed from ${scala3.nonEmpty} to ${other.scala3.nonEmpty}"
    )
    if scala3.nonEmpty
    then require(
      other.scala3.get.version == scala3.get.version,
      s"Scala 3 version changed from ${scala3.get.version} to ${other.scala3.get.version}"
    )
    else require(
      other.scala2.get.version == scala2.get.version,
      s"Scala 2 version changed from ${scala2.get.version} to ${other.scala2.get.version}"
    )

object ScalaLibrary:
  def getFromConfiguration(
    project: Project,
    configurationName: String
  ): ScalaLibrary =
    ScalaLibrary(
      scala3 = ScalaVersion.Scala3.scalaLibraryDependency.findInConfiguration(project, configurationName),
      scala2 = ScalaVersion.Scala2.scalaLibraryDependency.findInConfiguration(project, configurationName)
    )

  def getFromClasspath(classPath: Iterable[File]): ScalaLibrary =
    val result: ScalaLibrary = ScalaLibrary(
      scala3 = ScalaVersion.Scala3.scalaLibraryDependency.findInClassPath(classPath),
      scala2 = ScalaVersion.Scala2.scalaLibraryDependency.findInClassPath(classPath)
    )
    require(result.scala2.nonEmpty, "No Scala 2 library!")
    result
