package org.podval.tools.build

sealed trait ScalaVersion:
  final def isScalaVersionAcceptable(scalaVersion: Version): Boolean =
    isSameMajor(scalaVersion) &&
    isScalaVersionOfCorrectLength(scalaVersion)

  final def isSameMajor(version: Version): Boolean = version.simple.segmentInt(0) == versionMajor

  protected def versionMajor: Int

  def isScala3: Boolean

  final def scalaLibraryDependency: JavaDependency = ScalaLibrary.dependency(())
  protected def ScalaLibrary: JavaDependency.Maker

  // WTF?
  protected def isScalaVersionOfCorrectLength(scalaVersion: Version): Boolean

  // TODO require(isScalaVersionAcceptable)
  def versionSuffix(scalaVersion: Version): String

object ScalaVersion:
  val group: String = "org.scala-lang"

  def versionDefaults: Seq[Version] = Seq(
    ScalaVersion.Scala3         .versionDefault,
    ScalaVersion.Scala2.Scala213.versionDefault,
    ScalaVersion.Scala2.Scala212.versionDefault
  )

  def forVersion(scalaVersion: Version): ScalaVersion =
    if ScalaVersion.Scala3.isSameMajor(scalaVersion)
    then ScalaVersion.Scala3
    else ScalaVersion.Scala2

  def scalaLibraryDependencyWithVersion(scalaVersion: Version): Dependency.WithVersion = forVersion(scalaVersion)
    .scalaLibraryDependency
    .withVersion(scalaVersion)

  object Scala3 extends ScalaVersion:
    override protected def versionMajor: Int = 3
    override def isScala3: Boolean = true
    override def versionSuffix(scalaVersion: Version): String = scalaVersion.simple.segment(0)
    override protected def isScalaVersionOfCorrectLength(scalaVersion: Version): Boolean = true
    
    val versionDefault: Version = Version("3.7.0")

    override protected object ScalaLibrary extends JavaDependency.Maker:
      override def versionDefault: Version = Scala3.versionDefault
      override def group: String = ScalaVersion.group
      override def artifact: String = "scala3-library_3"
      override def description: String = "Scala 3 Library."

    // There is no Scala 2 equivalent
    object ScalaLibraryJS extends ScalaDependency.Maker:
      override def versionDefault: Version = Scala3.versionDefault
      override def group: String = ScalaVersion.group
      override def artifact: String = "scala3-library"
      override def description: String = "Scala 3 library in Scala.js."

  object Scala2 extends ScalaVersion:
    override protected def versionMajor: Int = 2
    override def isScala3: Boolean = false
    override def versionSuffix(scalaVersion: Version): String =
      scalaVersion.simple.segment(0) + "." + scalaVersion.simple.segment(1)
    override protected def isScalaVersionOfCorrectLength(scalaVersion: Version): Boolean = true
    
    // Scala 2 version used by Scala 3 from 3.0.0 to the current is 2.13.
    val majorAndMinor: Version = Version("2.13")
    
    override protected object ScalaLibrary extends JavaDependency.Maker:
      override def versionDefault: Version = Scala2.Scala213.versionDefault
      override def group: String = ScalaVersion.group
      override def artifact: String = "scala-library"
      override def description: String = "Scala 2 Library."

    object Scala213:
      val versionDefault: Version = Version("2.13.16")

    object Scala212:
      val versionDefault: Version = Version("2.12.20")
