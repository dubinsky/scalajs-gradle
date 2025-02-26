package org.podval.tools.test.framework

sealed trait OptionStyle:
  def toStrings(name: String, values: Array[String]): Seq[String]

object OptionStyle:
  case object NotSupported extends OptionStyle:
    override def toStrings(name: String, values: Array[String]): Seq[String] = Seq.empty

  case object OptionPerValue extends OptionStyle:
    override def toStrings(name: String, values: Array[String]): Seq[String] =
      values.toIndexedSeq.flatMap((value: String) => Seq(name, value))
      
  sealed trait ListOptionStyle extends OptionStyle:
    final override def toStrings(name: String, values: Array[String]): Seq[String] =
      if values.isEmpty
      then Seq.empty
      else toStrings(name, values.mkString(","))
      
    protected def toStrings(name: String, valuesString: String): Seq[String]
    
  case object ListWithEq extends ListOptionStyle:
    override protected def toStrings(name: String, valuesString: String): Seq[String] = Seq(s"$name=$valuesString")

  case object ListWithoutEq extends ListOptionStyle:
    override protected def toStrings(name: String, valuesString: String): Seq[String] = Seq(name, valuesString)
