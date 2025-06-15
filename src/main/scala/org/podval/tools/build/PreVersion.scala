package org.podval.tools.build

abstract class PreVersion derives CanEqual:
  def simple: Version
  def compound: CompoundVersion

  final override def equals(other: Any): Boolean = other match
    case that: PreVersion => this.toString == that.toString
    case _ => false

object PreVersion:
  private def isCompound(string: String): Boolean = string.contains('+')
  
  def apply(
    string: String,
    isVersionCompound: Boolean
  ): Option[PreVersion] =
    if isVersionCompound  &&  isCompound(string) then Some(CompoundVersion(string)) else
    if !isVersionCompound && !isCompound(string) then Some(Version        (string)) else
      None
