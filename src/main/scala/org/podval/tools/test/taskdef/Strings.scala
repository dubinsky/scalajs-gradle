package org.podval.tools.test.taskdef

object Strings extends Ops[String]("<never needed>"):
  object Many extends ArrayOps[String](Strings, "///")

  override protected def toStrings(value: String): Array[String] = Array(value)
  override protected def fromStrings(strings: Array[String]): String = strings(0)
  