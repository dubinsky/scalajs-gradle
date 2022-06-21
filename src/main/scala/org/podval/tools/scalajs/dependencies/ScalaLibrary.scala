package org.podval.tools.scalajs.dependencies

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.tasks.SourceSet
import org.opentorah.build.Gradle.*

final class ScalaLibrary(
  val scala3: Option[DependencyVersion],
  val scala2: Option[DependencyVersion]
):
  require(scala3.nonEmpty || scala2.nonEmpty, "No Scala library!")

  val isScala3: Boolean = scala3.nonEmpty

  def version: String = scala3.getOrElse(scala2.get).version

  def scala2versionMinor: String = scala2
    .map(_.versionMinor)
    .getOrElse(ScalaLibrary.scala2versionMinor(scala3.get.version))

object ScalaLibrary:
  val group: String = "org.scala-lang"

  object Scala2 extends SimpleDependency(group = group, nameBase = "scala-library")
  object Scala3 extends Scala3Dependency(group = group, nameBase = "scala3-library")

  object SJS:
    object Scala2 extends Scala2Dependency(group = group, nameBase = "scala-library_sjs1")
    object Scala3 extends Scala3Dependency(group = group, nameBase = "scala3-library_sjs1")

  // Note: Scala 2 minor version used by Scala 3 from 3.0.0 to the current is 2.13
  def scala2versionMinor(scala3version: String): String = "2.13"

  def getFromConfiguration(project: Project): ScalaLibrary =
    val implementation: Configuration = GradleUtil.getConfiguration(project, JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)
    ScalaLibrary(
      scala3 = Scala3.getFromConfiguration(implementation),
      scala2 = Scala2.getFromConfiguration(implementation)
    )

  def getFromClasspath(project: Project): ScalaLibrary =
    val mainScalaCompile: ScalaCompile = GradleUtil.getScalaCompile(project, project.getSourceSet(SourceSet.MAIN_SOURCE_SET_NAME))
    val result:ScalaLibrary = ScalaLibrary(
      scala3 = Scala3.getFromClasspath(mainScalaCompile.getClasspath),
      scala2 = Scala2.getFromClasspath(mainScalaCompile.getClasspath)
    )
    require(result.scala2.nonEmpty, "No Scala 2 library!")
    result
