package org.podval.tools.build

import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.tasks.JvmConstants
import scala.jdk.CollectionConverters.IterableHasAsScala
import java.io.File

final class ScalaLibrary private(
  val scala3: Option[ScalaVersion],
  val scala2: Option[ScalaVersion]
):
  override def toString: String = s"ScalaLibrary(scala3=$scala3, scala2=$scala2)"

  def scalaVersion: ScalaVersion = scala3.getOrElse(scala2.get)
  
  def verify(runtimeClasspathConfiguration: Configuration): Unit =
    val other: ScalaLibrary = ScalaLibrary.getFromClasspath(runtimeClasspathConfiguration.asScala)
    val configurationName: String = runtimeClasspathConfiguration.getName
    
    require(
      other.scala3.nonEmpty == scala3.nonEmpty,
      s"Scala 3 presence changed from ${scala3.nonEmpty} to ${other.scala3.nonEmpty} in configuration '$configurationName'."
    )
    if scala3.nonEmpty
    then require(
      other.scala3.get == scala3.get,
      s"Scala 3 version changed from ${scala3.get} to ${other.scala3.get} in configuration '$configurationName'."
    )
    else require(
      other.scala2.get == scala2.get,
      s"Scala 2 version changed from ${scala2.get} to ${other.scala2.get} in configuration '$configurationName'."
    )

object ScalaLibrary:
  def getFromConfiguration(configuration: Configuration): ScalaLibrary = ScalaLibrary(
    source = s"in configuration '${configuration.getName}'",
    mustHaveScala2 = false,
    scala3 = ScalaBinaryVersion.Scala3  .scalaLibraryDependency.findInConfiguration(configuration),
    scala2 = ScalaBinaryVersion.Scala213.scalaLibraryDependency.findInConfiguration(configuration)
  )

  def getFromClasspath(classPath: Iterable[File]): ScalaLibrary = ScalaLibrary(
    source = "on classpath " + classPath.mkString(", "),
    mustHaveScala2 = true,
    scala3 = ScalaBinaryVersion.Scala3  .scalaLibraryDependency.findInClassPath(classPath),
    scala2 = ScalaBinaryVersion.Scala213.scalaLibraryDependency.findInClassPath(classPath)
  )

  private def apply(
    source: String,
    mustHaveScala2: Boolean,
    scala3: Option[Dependency#WithVersion],
    scala2: Option[Dependency#WithVersion]
  ): ScalaLibrary =
    require(scala3.nonEmpty || scala2.nonEmpty, s"No Scala library $source")
    if mustHaveScala2 then require(scala2.nonEmpty, s"No Scala 2 library $source")

    new ScalaLibrary(
      scala3.map(dependency => ScalaVersion(dependency.version.simple)), 
      scala2.map(dependency => ScalaVersion(dependency.version.simple))
    )
    