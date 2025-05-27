package org.podval.tools.build

final class ScalaVersion private(val version: Version.Simple) derives CanEqual:
  override def toString: String = version.toString

  override def equals(other: Any): Boolean = other match
    case that: ScalaVersion => this.version == that.version
    case _ => false

  def binaryVersion: ScalaBinaryVersion = ScalaBinaryVersion.forScalaVersion(this)

  require(version.length >= binaryVersion.versionSuffixLength)

  def isScala3: Boolean = binaryVersion == ScalaBinaryVersion.Scala3

  def scalaLibraryDependencyWithVersion: Dependency.WithVersion = binaryVersion
    .scalaLibraryDependency
    .withVersion(version)

object ScalaVersion:
  def apply(version: Version.Simple): ScalaVersion = new ScalaVersion(version)
  def apply(version: String): ScalaVersion = new ScalaVersion(Version.Simple(version))
