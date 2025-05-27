package org.podval.tools.build

sealed abstract class Version derives CanEqual:
  def simple: Version.Simple
  def compound: Version.Compound

  final override def equals(other: Any): Boolean = other match
    case that: Version => this.toString == that.toString
    case _ => false

object Version:
  final class Simple private(segments: Array[String]) extends Version:
    override def toString: String = segments.mkString(".")
    override def simple: Simple = this
    override def compound: Version.Compound = throw ClassCastException(s"Version $this is not compound.")
    def length: Int = segments.length
    def take(n: Int): Simple = new Simple(segments.take(n))
    private def segment(index: Int): Int = segments(index).toInt
    def major: Int = segment(0)
    def minor: Int = segment(1)
    def patch: Int = segment(2)

  object Simple:
    def apply(string: String): Simple = new Simple(string.split("\\."))

  final class Compound(val left: Simple, val right: Simple) extends Version:
    override def toString: String = s"$left+$right"
    override def compound: Version.Compound = this
    override def simple: Simple = throw ClassCastException(s"Version $this is not simple.")

  object Compound:
    def apply(string: String): Compound =
      val plusIndex: Int = string.indexOf("+")
      new Compound(
        Simple(string.substring(0, plusIndex)),
        Simple(string.substring(plusIndex+1))
      )
  
  private def isCompound(string: String): Boolean = string.contains('+')
  
  def apply(
    string: String,
    isVersionCompound: Boolean
  ): Option[Version] =
    if isVersionCompound  &&  isCompound(string) then Some(Compound(string)) else
    if !isVersionCompound && !isCompound(string) then Some(Simple  (string)) else
      None
