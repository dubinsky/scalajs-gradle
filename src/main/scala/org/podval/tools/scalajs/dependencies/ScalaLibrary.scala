package org.podval.tools.scalajs.dependencies

import org.gradle.api.Project
import org.opentorah.build.Gradle.*

final class ScalaLibrary(
  val scala3: Option[DependencyVersion],
  val scala2: Option[DependencyVersion]
):
  require(scala3.nonEmpty || scala2.nonEmpty, "No Scala library!")
  def isScala3: Boolean = scala3.nonEmpty
  def isScala2: Boolean = scala3.isEmpty

  def verifyFromClasspath(project: Project): Unit =
    val fromClasspath: ScalaLibrary = ScalaLibrary.getFromClasspath(project)
    require(fromClasspath.isScala3 == isScala3, "Scala 3 presence changed")
    if isScala3
    then require(fromClasspath.scala3.get.version == scala3.get.version, "Scala 3 version changed")
    else require(fromClasspath.scala2.get.version == scala2.get.version, "Scala 2 version changed")

object ScalaLibrary:
  val group: String = "org.scala-lang"

  object Scala2    extends SimpleDependency(group = group, nameBase = "scala-library")
  object Scala3    extends Scala3Dependency(group = group, nameBase = "scala3-library")

  // Note: there is no Scala 2 equivalent
  object Scala3SJS extends Scala3Dependency(group = group, nameBase = "scala3-library_sjs1")

  // Note: Scala 2 minor version used by Scala 3 from 3.0.0 to the current is 2.13
  def scala2versionMinor(scala3version: String): String = "2.13"

  def getFromConfiguration(project: Project): ScalaLibrary = ScalaLibrary(
    scala3 = Scala3.getFromConfiguration(ConfigurationNames.implementation, project),
    scala2 = Scala2.getFromConfiguration(ConfigurationNames.implementation, project)
  )

  def getFromClasspath(project: Project): ScalaLibrary =
    val result: ScalaLibrary = ScalaLibrary(
      scala3 = Scala3.getFromClasspath(ConfigurationNames.implementation, project),
      scala2 = Scala2.getFromClasspath(ConfigurationNames.implementation, project)
    )
    require(result.scala2.nonEmpty, "No Scala 2 library!")
    result
