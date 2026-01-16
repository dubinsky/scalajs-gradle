package org.podval.tools.build

sealed abstract class ScalaVersion(val version: Version) derives CanEqual:
  require(binaryVersion.is(version))

  final override def toString: String = version.toString

  def binaryVersion: ScalaBinaryVersion
  
  final override def equals(other: Any): Boolean = other.asInstanceOf[Matchable] match
    case that: ScalaVersion => this.version == that.version
    case _ => false

  final def versionSuffix(isFull: Boolean): Version = if isFull then version else binaryVersionSuffix
  final def binaryVersionSuffix: Version = binaryVersion.prefix

  final def isScala3: Boolean = !isScala2
  def isScala2  : Boolean
  final def isScala212: Boolean = isScala2 && !isScala213
  def isScala213: Boolean

object ScalaVersion:
  final class Scala3(version: Version) extends ScalaVersion(version):
    override def binaryVersion: ScalaBinaryVersion = ScalaBinaryVersion.Scala3
    override def isScala2: Boolean = false
    override def isScala213: Boolean = false

  sealed abstract class Scala2(version: Version) extends ScalaVersion(version):
    final override def isScala2: Boolean = true

  final class Scala2_13(version: Version) extends Scala2(version):
    override def binaryVersion: ScalaBinaryVersion = ScalaBinaryVersion.Scala2_13
    override def isScala213: Boolean = true

  final class Scala2_12(version: Version) extends Scala2(version):
    override def binaryVersion: ScalaBinaryVersion = ScalaBinaryVersion.Scala2_12
    override def isScala213: Boolean = false

  def apply(string: String): ScalaVersion = ScalaVersion(Version(string))

  def apply(version: Version): ScalaVersion =
    if ScalaBinaryVersion.Scala3.is(version) then Scala3(version)
    else if ScalaBinaryVersion.Scala2_13.is(version) then Scala2_13(version)
    else Scala2_12(version)
