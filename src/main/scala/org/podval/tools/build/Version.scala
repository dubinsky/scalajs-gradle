package org.podval.tools.build

final class Version private(segments: Array[String]) extends PreVersion:
  override def toString: String = segments.mkString(".")
  override def simple: Version = this
  override def compound: CompoundVersion = throw ClassCastException(s"Version $this is not compound.")

  def length: Int = segments.length
  def take(n: Int): Version = new Version(segments.take(n))
  private def segment(index: Int): Int = segments(index).toInt
  def major: Int = segment(0)
  def minor: Int = segment(1)
  def patch: Int = segment(2)

object Version:
  def apply(string: String): Version = new Version(string.split("\\."))
