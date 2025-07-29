package org.podval.tools.build

final class ScalaVersion(val version: Version) derives CanEqual:
  require(version.length >= binaryVersion.versionSuffixLength)

  override def toString: String = version.toString

  override def equals(other: Any): Boolean = other match
    case that: ScalaVersion => this.version == that.version
    case _ => false

  def isScala3: Boolean = binaryVersion.isScala3
  def isScala2: Boolean = binaryVersion.isScala2

  def binaryVersion: ScalaBinaryVersion = ScalaBinaryVersion.forVersion(version)
