package org.podval.tools.build

sealed trait ScalaVersion:
  final def isScalaVersionAcceptable(scalaVersion: Version): Boolean =
    isSameMajor(scalaVersion) &&
    isScalaVersionOfCorrectLength(scalaVersion)

  final def isSameMajor(version: Version): Boolean = version.major == versionMajor

  protected def versionMajor: Int

  // WTF?
  protected def isScalaVersionOfCorrectLength(scalaVersion: Version): Boolean

  // TODO require(isScalaVersionAcceptable)
  def versionSuffix(scalaVersion: Version): String

  def scalaVersion(library: ScalaLibrary): Version

object ScalaVersion:
  object Scala3 extends ScalaVersion:
    override protected def versionMajor: Int = 3

    override protected def isScalaVersionOfCorrectLength(scalaVersion: Version): Boolean = true

    override def versionSuffix(scalaVersion: Version): String = scalaVersion.major.toString

    val versionDefault: Version = Version("3.6.3")
    
    override def scalaVersion(library: ScalaLibrary): Version =
      library.scala3.get.version

    // Note: Scala 2 minor version used by Scala 3 from 3.0.0 to the current is 2.13
    val scala2versionMinor: Version = Version("2.13")

    def scala2versionMinor(scala3version: Version): Version = scala2versionMinor

  object Scala2 extends ScalaVersion:
    override protected def versionMajor: Int = 2

    override protected def isScalaVersionOfCorrectLength(scalaVersion: Version): Boolean = true

    override def versionSuffix(scalaVersion: Version): String = scalaVersion.majorAndMinorString
    
    override def scalaVersion(library: ScalaLibrary): Version =
      library.scala2.map(_.version).getOrElse(
        Scala3.scala2versionMinor(library.scala3.get.version)
      )

    object Scala213:
      val versionDefault: Version = Version("2.13.16")

    object Scala212:
      val versionDefault: Version = Version("2.12.20")
