package org.podval.tools.build

sealed trait ScalaBinaryVersion extends JavaDependency derives CanEqual:
  final override def toString: String = versionSuffix.toString
  final override def group: String = ScalaBinaryVersion.group
  final override def versionDefault: Version = scalaVersionDefault.version
  final def versionSuffix: Version = versionDefault.take(versionSuffixLength)

  def versionMajor: Int
  def versionSuffixLength: Int
  def scalaVersionDefault: ScalaVersion

  def isScala3  : Boolean
  def isScala2  : Boolean
  def isScala213: Boolean
  def isScala212: Boolean

object ScalaBinaryVersion:
  val group: String = "org.scala-lang"

  def forVersion(version: Version): ScalaBinaryVersion =
    if version.major == Scala3.versionMajor
    then Scala3
    else
      require(version.major == Scala2.versionMajor)
      if version.minor == Scala2_13.versionMinor
      then 
        Scala2_13
      else
        require(version.minor == Scala2_12.versionMinor)
        Scala2_12

  def all: Seq[ScalaBinaryVersion] = Seq(Scala3, Scala2_13, Scala2_12)
  
  object Scala3 extends ScalaBinaryVersion:
    override def versionMajor: Int = 3
    override def versionSuffixLength: Int = 1
    override def artifact: String = "scala3-library_3"
    override def description: String = "Scala 3 Library."
    override val scalaVersionDefault: ScalaVersion = ScalaVersion("3.7.3")
    override def isScala3: Boolean = true
    override def isScala2: Boolean = false
    override def isScala213: Boolean = false
    override def isScala212: Boolean = false

  private object Scala2:
    def versionMajor: Int = 2

  sealed trait Scala2 extends ScalaBinaryVersion:
    final override def versionMajor: Int = Scala2.versionMajor
    final override def versionSuffixLength: Int = 2
    override def artifact: String = "scala-library"
    override def description: String = "Scala 2 Library."
    def versionMinor: Int
    override def isScala3: Boolean = false
    override def isScala2: Boolean = true

  object Scala2_13 extends Scala2:
    override def versionMinor: Int = 13
    override def isScala213: Boolean = true
    override def isScala212: Boolean = false
    val scalaVersionDefault: ScalaVersion = ScalaVersion("2.13.16")

  object Scala2_12 extends Scala2:
    override def versionMinor: Int = 12
    override def isScala213: Boolean = false
    override def isScala212: Boolean = true
    val scalaVersionDefault: ScalaVersion = ScalaVersion("2.12.20")
