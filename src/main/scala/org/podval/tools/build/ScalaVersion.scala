package org.podval.tools.build

sealed trait ScalaVersion:
  final def isScalaVersionAcceptable(scalaVersion: Version): Boolean =
    isSameMajor(scalaVersion) &&
    isScalaVersionOfCorrectLength(scalaVersion)

  final def isSameMajor(version: Version): Boolean = version.major == versionMajor

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

  def forVersion(scalaVersion: Version): ScalaVersion =
    if ScalaVersion.Scala3.isSameMajor(scalaVersion)
    then ScalaVersion.Scala3
    else ScalaVersion.Scala2
    
  object Scala3 extends ScalaVersion:
    override protected def versionMajor: Int = 3
    override def isScala3: Boolean = true
    override def versionSuffix(scalaVersion: Version): String = scalaVersion.major.toString
    override protected def isScalaVersionOfCorrectLength(scalaVersion: Version): Boolean = true
    
    val versionDefault: Version = Version("3.6.3")

    override protected object ScalaLibrary extends JavaDependency.Maker:
      override def versionDefault: Version = Scala3.versionDefault
      override def group: String = ScalaVersion.group
      override def artifact: String = "scala3-library_3"

    // Note: there is no Scala 2 equivalent
    object ScalaLibraryJS extends ScalaDependency.Maker:
      override def versionDefault: Version = Scala3.versionDefault
      override def group: String = ScalaVersion.group
      override def artifact: String = "scala3-library"

  object Scala2 extends ScalaVersion:
    override protected def versionMajor: Int = 2
    override def isScala3: Boolean = false
    override def versionSuffix(scalaVersion: Version): String = scalaVersion.majorAndMinorString
    override protected def isScalaVersionOfCorrectLength(scalaVersion: Version): Boolean = true
    
    // Note: Scala 2 version used by Scala 3 from 3.0.0 to the current is 2.13
    val majorAndMinor: Version = Version("2.13")
    
    override protected object ScalaLibrary extends JavaDependency.Maker:
      override def versionDefault: Version = Scala2.Scala213.versionDefault
      override def group: String = ScalaVersion.group
      override def artifact: String = "scala-library"

    object Scala213:
      val versionDefault: Version = Version("2.13.16")

    object Scala212:
      val versionDefault: Version = Version("2.12.20")
