package org.podval.tools.build

final class ScalaVersion(val version: Version) derives CanEqual:
  require(version.length >= binaryVersion.versionSuffixLength)

  override def toString: String = version.toString

  override def equals(other: Any): Boolean = other.asInstanceOf[Matchable] match
    case that: ScalaVersion => this.version == that.version
    case _ => false

  private def binaryVersion: ScalaBinaryVersion = ScalaBinaryVersion.forVersion(version)

  def versionSuffix(isFull: Boolean): Version = if isFull then version else binaryVersionSuffix
  def binaryVersionSuffix: Version = binaryVersion.versionSuffix

  def isScala3  : Boolean = binaryVersion.isScala3
  def isScala2  : Boolean = binaryVersion.isScala2
  def isScala213: Boolean = binaryVersion.isScala213
  def isScala212: Boolean = binaryVersion.isScala212

object ScalaVersion:
  def apply(string: String): ScalaVersion = ScalaVersion(Version(string))
  def apply(version: Version): ScalaVersion = new ScalaVersion(version)
