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
  val group: String = "org.scala-lang"
  
  abstract class Scala(artifact: String) extends JavaDependency(group = group, artifact):
    def versionMajor: Int
    def getScalaVersion(library: ScalaLibrary): Version

  object Scala2 extends Scala("scala-library"):
    override def versionMajor: Int = 2
    val versionDefault13: Version = Version("2.13.16")
    val versionDefault12: Version = Version("2.12.20")

    override def getScalaVersion(library: ScalaLibrary): Version =
      library.scala2.map(_.version).getOrElse(Scala3.scala2version(library.scala3.get.version))

  object Scala3 extends Scala(artifact = "scala3-library_3"):
    override def versionMajor: Int = 3
    val versionDefault: Version = Version("3.6.3")

    override def getScalaVersion(library: ScalaLibrary): Version = library.scala3.get.version

    // Note: Scala 2 minor version used by Scala 3 from 3.0.0 to the current is 2.13
    def scala2version(scala3version: Version): Version = Version("2.13")

  def forVersion(version: Version): Dependency.WithVersion =
    (if version.major == Scala3.versionMajor then Scala3 else Scala2).withVersion(version)

  // Note: there is no Scala 2 equivalent
  object Scala3SJS extends ScalaDependency.Scala3(group = group, artifact = "scala3-library", isScalaJS = true)

  def getFromConfiguration(configuration: Configuration): ScalaLibrary =
    ScalaLibrary(
      scala3 = Scala3.findInConfiguration(configuration),
      scala2 = Scala2.findInConfiguration(configuration)
    )

  def getFromClasspath(classPath: Iterable[File]): ScalaLibrary =
    val result: ScalaLibrary = ScalaLibrary(
      scala3 = Scala3.findInClassPath(classPath),
      scala2 = Scala2.findInClassPath(classPath)
    )
    require(result.scala2.nonEmpty, "No Scala 2 library!")
    result
