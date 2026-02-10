package org.podval.tools.util

object Strings:
  def split(what: String, on: Char): (String, Option[String]) = what.lastIndexOf(on) match
    case -1 => (what, None)
    case index => (what.substring(0, index), Some(what.substring(index+1)))

  def dropPrefixIfPresent(string: String, prefix: String): String = detectPrefix(string, prefix)
    .getOrElse(string)

  def dropPrefix(string: String, prefix: String): String = detectPrefix(string, prefix)
    .getOrElse(throw IllegalArgumentException(s"String '$string' doesn't start with '$prefix'"))

  def detectPrefix(string: String, prefix: String): Option[String] =
    if string.startsWith(prefix) then Some(string.substring(prefix.length)) else None

  def prefix(prefix: String, what: Option[String]): String = what.fold("")(prefix + _)

  def toString[T](strings: Iterable[T], f: T => String): String = strings.map(f).mkString(", ")

  def toString(strings: Seq[String]): String = strings.mkString("", "\n", "\n")

  def splice(
    in: Seq[String],
    boundary: String,
    patch: Seq[String]
  ): Seq[String] =
    in.takeWhile(_ != boundary) ++
    Seq(boundary) ++
    patch ++
    in.dropWhile(_ != boundary).tail.dropWhile(_ != boundary)
