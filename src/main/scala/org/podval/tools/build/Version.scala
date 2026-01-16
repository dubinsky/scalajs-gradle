package org.podval.tools.build

import org.gradle.api.provider.Property

final class Version private(val segments: Array[String]) extends Version.Pre:
  override def toString: String = segments.mkString(".")
  override def version: Version = this

  def length: Int = segments.length
  def take(n: Int): Version = new Version(segments.take(n))

  def startsWith(suffix: Version): Boolean =
    (length >= suffix.length) &&
    0.until(suffix.length).forall(index => segments(index) == suffix.segments(index))

  private def segment(index: Int): Int = segments(index).toInt
  def major: Int = segment(0)
  def minor: Int = segment(1)
  def patch: Int = segment(2)

  def after(that: Version): Boolean =
    (this.major > that.major) ||
    (this.major == that.major && this.major > that.minor) ||
    (this.major == that.major && this.major == that.minor && this.patch > that.patch)
    
object Version:
  abstract class Pre derives CanEqual:
    def version: Version

    final override def equals(other: Any): Boolean = other.asInstanceOf[Matchable] match
      case that: Pre => this.toString == that.toString
      case _ => false

  def apply(property: Property[String]): Option[Version] = Option(property.getOrNull).map(apply)
  
  def apply(string: String): Version = new Version(string.split("\\."))

  private final class WithScalaVersion(
    scalaVersion: ScalaVersion,
    override val version: Version
  ) extends Pre:
    override def toString: String = s"$scalaVersion+$version"

  def compose(isVersionCompound: Boolean, scalaVersion: ScalaVersion, version: Version): Pre =
    if !isVersionCompound then version else WithScalaVersion(scalaVersion, version)
  
  def parse(isVersionCompound: Boolean, string: String): Option[Pre] =
    Option.when(isVersionCompound == string.contains('+')):
      if !isVersionCompound then Version(string) else WithScalaVersion(
        ScalaVersion(string.takeWhile(_ != '+')),
        Version     (string.dropWhile(_ != '+').tail)
      )
