package org.podval.tools.test.framework

import org.podval.tools.util.Scala212Collections.{arrayFlatMap, arrayMkString}

sealed trait OptionStyle:
  def toStrings(name: String, values: Array[String]): Array[String]

object OptionStyle:
  case object NotSupported extends OptionStyle:
    override def toStrings(name: String, values: Array[String]): Array[String] = Array.empty

  case object OptionPerValue extends OptionStyle:
    override def toStrings(name: String, values: Array[String]): Array[String] =
      arrayFlatMap(values, (value: String) => Array(name, value))

  sealed trait ListOptionStyle extends OptionStyle:
    protected def toStrings(name: String, valuesString: String): Array[String]
    final override def toStrings(name: String, values: Array[String]): Array[String] =
      if values.length == 0
      then Array.empty
      else toStrings(name, arrayMkString(values, "", ",", ""))

  case object ListWithEq extends ListOptionStyle:
    override protected def toStrings(name: String, valuesString: String): Array[String] = Array(s"$name=$valuesString")

  case object ListWithoutEq extends ListOptionStyle:
    override protected def toStrings(name: String, valuesString: String): Array[String] = Array(name, valuesString)
