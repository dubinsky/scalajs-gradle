package org.podval.tools.build

import org.gradle.api.artifacts.Configuration
import java.io.File

final class ScalaLibrary(
  val scala3: Option[Dependency.WithVersion],
  val scala2: Option[Dependency.WithVersion]
):
  require(scala3.nonEmpty || scala2.nonEmpty, "No Scala library!")
  def isScala3: Boolean = scala3.nonEmpty
  def isScala2: Boolean = scala3.isEmpty

  override def toString: String = s"ScalaLibrary(scala3=${scala3.map(_.version)}, scala2=${scala2.map(_.version)})"

  def verify(other: ScalaLibrary): Unit =
    require(
      other.isScala3 == isScala3,
      s"Scala 3 presence changed from $isScala3 to ${other.isScala3}"
    )
    if isScala3
    then require(
      other.scala3.get.version == scala3.get.version,
      s"Scala 3 version changed from ${scala3.get.version} to ${other.scala3.get.version}"
    )
    else require(
      other.scala2.get.version == scala2.get.version,
      s"Scala 2 version changed from ${scala2.get.version} to ${other.scala2.get.version}"
    )

object ScalaLibrary:
  def getFromConfiguration(configuration: Configuration): ScalaLibrary =
    ScalaLibrary(
      scala3 = ScalaLibraryDependency.Scala3.findInConfiguration(configuration),
      scala2 = ScalaLibraryDependency.Scala2.findInConfiguration(configuration)
    )

  def getFromClasspath(classPath: Iterable[File]): ScalaLibrary =
    val result: ScalaLibrary = ScalaLibrary(
      scala3 = ScalaLibraryDependency.Scala3.findInClassPath(classPath),
      scala2 = ScalaLibraryDependency.Scala2.findInClassPath(classPath)
    )
    require(result.scala2.nonEmpty, "No Scala 2 library!")
    result
