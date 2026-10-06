package org.podval.tools.build

import org.gradle.api.provider.Property
import org.podval.tools.util.Strings
import scala.annotation.tailrec

final class Version private(val segments: Array[String]) extends Version.Pre with Ordered[Version]:
  override def toString: String = segments.mkString(".")
  override def version: Version = this
  override def nonCompound: Version = this

  def length: Int = segments.length
  def take(n: Int): Version = new Version(segments.take(n))

  def int(index: Int): Int = segments(index).toInt

  def startsWith(prefix: Version): Boolean =
    (length >= prefix.length) &&
    0.until(prefix.length).forall(index => segments(index) == prefix.segments(index))

  override def compare(that: Version): Int =
    // A segment may be "0-RC1". The numeric part orders first.
    // A bare number is newer than the same number with a pre-release suffix.
    def segmentKey(segment: String): (Int, String) =
      val digits: String = segment.takeWhile(_.isDigit)
      val number: Int = if digits.isEmpty then -1 else digits.toInt
      (number, segment.substring(digits.length))

    @tailrec def compare(index: Int): Int = (this.length == index, that.length == index) match
      case (true , true ) =>  0
      case (true , false) => -1
      case (false, true ) =>  1
      case _ =>
        val (leftNumber, leftRest) = segmentKey(segments(index))
        val (rightNumber, rightRest) = segmentKey(that.segments(index))
        Ordering.Int.compare(leftNumber, rightNumber) match
          case 0 =>
            val restOrder: Int =
              if leftRest.isEmpty && rightRest.isEmpty then 0
              else if leftRest.isEmpty then 1
              else if rightRest.isEmpty then -1
              else leftRest.compareTo(rightRest)
            if restOrder == 0 then compare(index + 1) else restOrder
          case result => result

    compare(0)

object Version:
  abstract class Pre derives CanEqual:
    def version: Version
    def nonCompound: Version

    final override def equals(other: Any): Boolean = other.asInstanceOf[Matchable] match
      case that: Pre => this.toString == that.toString
      case _ => false

  def apply(property: Property[String]): Option[Version] = Option(property.getOrNull).map(apply)
  
  def apply(string: String): Version = new Version(string.split("\\."))

  private final class Compound(
    scalaVersion: ScalaVersion,
    override val version: Version
  ) extends Pre:
    override def toString: String = s"$scalaVersion+$version"
    override def nonCompound: Nothing = throw IllegalArgumentException(s"Version $this is compound but non-compound version is required")

  def compose(isVersionCompound: Boolean, scalaVersion: ScalaVersion, version: Version): Pre =
    if !isVersionCompound
    then version
    else Compound(scalaVersion, version)
  
  def parse(isVersionCompound: Boolean, string: String): Option[Pre] =
    val (before: String, after: Option[String]) = Strings.split(string, '+')
    Option.when(isVersionCompound == after.isDefined):
      after match
        case None => Version(before)
        case Some(after) => Compound(ScalaVersion(Version(before)), Version(after))
