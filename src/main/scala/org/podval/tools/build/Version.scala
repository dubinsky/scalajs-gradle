package org.podval.tools.build

sealed abstract class Version derives CanEqual:
  def simple: Version.Simple
  def compound: Version.Compound

  final def compound(version: Version): Version.Compound = Version.Compound(simple, version.simple)

  final override def equals(other: Any): Boolean = other match
    case that: Version => this.toString == that.toString
    case _ => false

object Version:
  final class Simple(override val toString: String) extends Version:
    override def simple: Simple = this
    override def compound: Version.Compound = throw ClassCastException(s"Version $this is not compound.")
    private val segments: Array[String] = toString.split("\\.")
    def segment(index: Int): String = segments(index)
    def segmentInt(index: Int): Int = segments(index).toInt
      
  final class Compound(val left: Simple, val right: Simple) extends Version:
    override def toString: String = s"$left+$right"
    override def compound: Version.Compound = this
    override def simple: Simple = throw ClassCastException(s"Version $this is not simple.")
    
  def apply(string: String): Version =
    val plusIndex: Int = string.indexOf("+")
    if plusIndex == -1
    then Simple(string)
    else Compound(
      Simple(string.substring(0, plusIndex)),
      Simple(string.substring(plusIndex+1))
    )
