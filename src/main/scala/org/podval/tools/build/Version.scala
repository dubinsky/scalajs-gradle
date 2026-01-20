package org.podval.tools.build

import org.gradle.api.provider.Property

import scala.annotation.tailrec

final class Version private(val segments: Array[String]) extends Version.Pre with Ordered[Version]:
  override def toString: String = segments.mkString(".")
  override def version: Version = this

  def length: Int = segments.length
  def take(n: Int): Version = new Version(segments.take(n))

  def int(index: Int): Int = segments(index).toInt

  def startsWith(prefix: Version): Boolean =
    (length >= prefix.length) &&
    0.until(prefix.length).forall(index => segments(index) == prefix.segments(index))

  override def compare(that: Version): Int =
    @tailrec def compare(index: Int): Int = (this.length == index, that.length == index) match
      case (true , true ) =>  0
      case (true , false) => -1
      case (false, true ) =>  1
      case _ => Ordering.Int.compare(this.int(index), that.int(index)) match
        case 0 => compare(index+1)
        case result => result

    compare(0)

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
