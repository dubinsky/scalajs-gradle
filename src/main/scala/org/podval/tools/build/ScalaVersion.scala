package org.podval.tools.build

final class ScalaVersion(val version: Version) derives CanEqual:
  require(version.length >= binaryVersion.versionSuffixLength)

  override def toString: String = version.toString

  override def equals(other: Any): Boolean = other match
    case that: ScalaVersion => this.version == that.version
    case _ => false

  def binaryVersion: ScalaBinaryVersion = ScalaBinaryVersion.forVersion(version)

  def binary2Version: ScalaBinaryVersion =
    if !isScala3 then binaryVersion else ScalaBinaryVersion.Scala2.P13

  def isScala3: Boolean = binaryVersion == ScalaBinaryVersion.Scala3
