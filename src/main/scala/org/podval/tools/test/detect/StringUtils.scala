package org.podval.tools.test.detect

import scala.annotation.tailrec

// Note: based on org.apache.commons.lang3.StringUtils
object StringUtils:
  /*
  Gets the substring after the last occurrence of a separator.
  A null string input will return null.
  An empty ("") string input will return the empty string.
  An empty or null separator will return the empty string if the input string is not null.
  If nothing is found, the empty string is returned.
  */
  def substringAfterLast(string: String, separator: String): String =
    if string == null then null else if string.isEmpty then "" else
      val index: Int = string.lastIndexOf(separator)
      if index == -1 then ""
      else string.substring(index + separator.length)

  /*
  Splits the provided text into an array, separator specified, preserving all tokens,
  including empty tokens created by adjacent separators.

  A null input String returns null. null splits on whitespace
  */
  def splitPreserveAllTokens(string: String, separator: Char): Array[String] =
    @tailrec def split(acc: Seq[String], string: String): Seq[String] =
      val (token: String, tail: String) = string.span(_ != separator)
      val newAcc: Seq[String] = acc :+ token
      if tail.isEmpty then newAcc else split(newAcc, tail.drop(1))

    if string == null then null else if string.isEmpty then Array.empty
    else split(Seq.empty, string).toArray
