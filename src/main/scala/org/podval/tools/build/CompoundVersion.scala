package org.podval.tools.build

final class CompoundVersion(val left: Version, val right: Version) extends PreVersion:
  override def toString: String = s"$left+$right"
  override def compound: CompoundVersion = this
  override def simple: Version = throw ClassCastException(s"Version $this is not simple.")

object CompoundVersion:
  def apply(string: String): CompoundVersion =
    val plusIndex: Int = string.indexOf("+")
    new CompoundVersion(
      Version(string.substring(0, plusIndex)),
      Version(string.substring(plusIndex + 1))
    )
