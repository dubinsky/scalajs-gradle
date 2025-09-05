package org.podval.tools.platform

object Strings:
  def split(what: String, on: Char): (String, Option[String]) = what.lastIndexOf(on) match
    case -1 => (what, None)
    case index => (what.substring(0, index), Some(what.substring(index+1)))

  def dropPrefixIfPresent(string: String, prefix: String): String =
    if string.startsWith(prefix) then string.substring(prefix.length) else string

  def dropPrefix(string: String, prefix: String): String =
    if string.startsWith(prefix) then string.substring(prefix.length)
    else throw IllegalArgumentException(s"String '$string' doesn't start with '$prefix'")

  def prefix(prefix: String, what: Option[String]): String = what.fold("")(prefix + _)

  def splice(
    in: Seq[String],
    boundary: String,
    patch: Seq[String]
  ): Seq[String] =
    in.takeWhile(_ != boundary) ++
    Seq(boundary) ++
    patch ++
    in.dropWhile(_ != boundary).tail.dropWhile(_ != boundary)
