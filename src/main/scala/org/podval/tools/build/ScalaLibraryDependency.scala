package org.podval.tools.build

abstract class ScalaLibraryDependency(artifact: String) extends JavaDependency(
  group = ScalaLibraryDependency.group, 
  artifact
):
  final def is(version: Version): Boolean = version.major == versionMajor
  
  protected def versionMajor: Int

  def scalaVersion(library: ScalaLibrary): Version

object ScalaLibraryDependency:
  val group: String = "org.scala-lang"
  
  def forVersion(version: Version): ScalaLibraryDependency = 
    if Scala3.is(version) then Scala3 else Scala2
  
  def withVersion(version: Version): Dependency.WithVersion =
    forVersion(version).withVersion(version)

  object Scala3 extends ScalaLibraryDependency(artifact = "scala3-library_3"):
    override protected def versionMajor: Int = 3

    val versionDefault: Version = Version("3.6.3")

    override def scalaVersion(library: ScalaLibrary): Version = 
      library.scala3.get.version

    // Note: Scala 2 minor version used by Scala 3 from 3.0.0 to the current is 2.13
    val scala2versionMinor: Version = Version("2.13")

    def scala2versionMinor(scala3version: Version): Version = scala2versionMinor
  
    // Note: there is no Scala 2 equivalent
    object ScalaJS extends ScalaDependency.Scala3(
      group = ScalaLibraryDependency.group, 
      artifact = "scala3-library", 
      isScalaJS = true
    )

  object Scala2 extends ScalaLibraryDependency("scala-library"):
    override protected def versionMajor: Int = 2

    val versionDefault13: Version = Version("2.13.16")
    val versionDefault12: Version = Version("2.12.20")

    override def scalaVersion(library: ScalaLibrary): Version =
      library.scala2.map(_.version).getOrElse(
        Scala3.scala2versionMinor(library.scala3.get.version)
      )
