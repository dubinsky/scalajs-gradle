package org.podval.tools.build

sealed trait ScalaBinaryVersion extends JavaDependencyMaker derives CanEqual:
  final override def toString: String = versionSuffix.toString
  final override def group: String = ScalaBinaryVersion.group
  final override def versionDefault: Version = scalaVersionDefault.version
  final def versionSuffix: Version = versionDefault.take(versionSuffixLength)

  def versionMajor: Int
  def versionSuffixLength: Int
  def scalaVersionDefault: ScalaVersion

object ScalaBinaryVersion:
  val group: String = "org.scala-lang"

  def forVersion(version: Version): ScalaBinaryVersion =
    if version.major == Scala3.versionMajor
    then Scala3
    else
      require(version.major == Scala2.versionMajor)
      if version.minor == Scala2.P13.versionMinor
      then 
        Scala2.P13
      else
        require(version.minor == Scala2.P12.versionMinor)
        Scala2.P12

  def all: Seq[ScalaBinaryVersion] = Seq(Scala3, Scala2.P13, Scala2.P12)
  
  object Scala3 extends ScalaBinaryVersion:
    override def versionMajor: Int = 3
    override def versionSuffixLength: Int = 1
    override def artifact: String = "scala3-library_3"
    override def description: String = "Scala 3 Library."
    override def scalaVersionDefault: ScalaVersion = Version("3.7.1").toScalaVersion
  
  sealed trait Scala2 extends ScalaBinaryVersion:
    final override def versionMajor: Int = Scala2.versionMajor
    final override def versionSuffixLength: Int = 2
    override def artifact: String = "scala-library"
    override def description: String = "Scala 2 Library."
    def versionMinor: Int

  object Scala2:
    def versionMajor: Int = 2

    object P13 extends Scala2:
      override def versionMinor: Int = 13
      val scalaVersionDefault: ScalaVersion = Version("2.13.16").toScalaVersion
  
    object P12 extends Scala2:
      override def versionMinor: Int = 12
      val scalaVersionDefault: ScalaVersion = Version("2.12.20").toScalaVersion
