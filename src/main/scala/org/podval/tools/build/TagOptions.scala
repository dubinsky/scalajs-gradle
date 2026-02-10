package org.podval.tools.build

import org.podval.tools.util.Scala212Collections.{arrayConcat, arrayFlatMap, writeStrings}

final class TagOptions(
  style: TagOptions.Style,
  includeTagsOption: String,
  excludeTagsOption: String
):
  def args(
    includeTags: Array[String],
    excludeTags: Array[String]
  ): Array[String] = arrayConcat(
    style.toStrings(includeTagsOption, includeTags),
    style.toStrings(excludeTagsOption, excludeTags),
  )

object TagOptions:
  sealed trait Style:
    def toStrings(name: String, values: Array[String]): Array[String]

    final def apply(
      includeTagsOption: String,
      excludeTagsOption: String
    ): Some[TagOptions] = Some(TagOptions(
      this,
      includeTagsOption,
      excludeTagsOption
    ))
  
  case object OptionPerValue extends Style:
    override def toStrings(name: String, values: Array[String]): Array[String] =
      arrayFlatMap(values, (value: String) => Array(name, value))

  sealed trait ListOptionStyle extends Style:
    protected def toStrings(name: String, valuesString: String): Array[String]

    final override def toStrings(name: String, values: Array[String]): Array[String] =
      if values.length == 0
      then Array.empty
      else toStrings(name, writeStrings(values, ","))

  case object ListWithEq extends ListOptionStyle:
    override protected def toStrings(name: String, valuesString: String): Array[String] = Array(s"$name=$valuesString")

  case object ListWithoutEq extends ListOptionStyle:
    override protected def toStrings(name: String, valuesString: String): Array[String] = Array(name, valuesString)
