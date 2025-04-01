package org.podval.tools.test.taskdef

import org.podval.tools.util.Scala212Collections.{arrayMkString, arrayZipForAll}

abstract class Ops[T](separator: String):
  final def equal(left: T, right: T): Boolean = arrayZipForAll(toStrings(left), toStrings(right), _ == _)
  
  final def write(value: T): String = arrayMkString(toStrings(value), "", separator, "")

  protected def toStrings(value: T): Array[String]

  // Thank you, sjrd, for the split trivia!
  // (https://github.com/scala-js/scala-js/pull/5132#discussion_r1967584316)
  final def read(string: String): T = fromStrings(string.split(separator, -1))

  protected def fromStrings(strings: Array[String]): T

object Ops:
  def toString(boolean: Boolean): String = boolean.toString

  def toBoolean(string: String): Boolean = string == "true"
