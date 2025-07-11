package org.podval.tools.build

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.tasks.JvmConstants
import org.podval.tools.gradle.{Configurations, GradleClasspath}
import scala.jdk.CollectionConverters.IterableHasAsScala
import java.io.File

final class ScalaLibrary private(
  val scala3: Option[ScalaVersion],
  val scala2: Option[ScalaVersion]
):
  override def toString: String = s"ScalaLibrary(scala3=$scala3, scala2=$scala2)"

  def scalaVersion: ScalaVersion = scala3.getOrElse(scala2.get)
  
  def verify(project: Project): Unit =
    val runtimeClasspathConfiguration: Configuration = Configurations.runtimeClasspath(project)
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
  def getFromImplementationConfiguration(project: Project): ScalaLibrary =
    val implementation: Configuration = Configurations.implementation(project)
    ScalaLibrary(
      source = s"in configuration '${implementation.getName}'",
      mustHaveScala2 = false,
      find = _.findInConfiguration(implementation)
    )

  def getFromClasspath: ScalaLibrary = getFromClasspath(GradleClasspath.collect(this))
  
  private def getFromClasspath(classPath: Iterable[File]): ScalaLibrary = ScalaLibrary(
    source = s"on classpath ${classPath.mkString(", ")}",
    mustHaveScala2 = true,
    find = _.findInClasspath(classPath)
  )

  private def apply(
    source: String,
    mustHaveScala2: Boolean,
    find: JavaDependency => Option[Dependency#WithVersion]
  ): ScalaLibrary =
    val scala3: Option[Dependency#WithVersion] = find(ScalaBinaryVersion.Scala3    .dependency)
    val scala2: Option[Dependency#WithVersion] = find(ScalaBinaryVersion.Scala2.P13.dependency)

    require(scala3.nonEmpty || scala2.nonEmpty, s"No Scala library $source.")
    if mustHaveScala2 then require(scala2.nonEmpty, s"No Scala 2 library $source.")

    def scalaVersion(dependencyWithVersion: Dependency#WithVersion): ScalaVersion =
      dependencyWithVersion.version.simple.toScalaVersion

    new ScalaLibrary(
      scala3.map(scalaVersion),
      scala2.map(scalaVersion)
    )
    