package org.podval.tools.test.taskdef

import org.podval.tools.util.Scala212Collections.arrayZipForAll

abstract class Ops[T](separator: String):
  final def equal(left: T, right: T): Boolean = arrayZipForAll(toStrings(left), toStrings(right), _ == _)
  
  final def write(value: T): String = toStrings(value).mkString(separator)

  protected def toStrings(value: T): Array[String]

  final def read(string: String): T = fromStrings(string.split(separator))

  protected def fromStrings(strings: Array[String]): T

object Ops:
  def toString(boolean: Boolean): String = boolean.toString

  def toBoolean(string: String): Boolean = string == "true"
