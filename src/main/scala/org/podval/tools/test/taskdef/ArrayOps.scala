package org.podval.tools.test.taskdef

import org.podval.tools.util.Scala212Collections.{arrayMap, arrayMkString}
import scala.reflect.ClassTag

class ArrayOps[T: ClassTag](ops: Ops[T], separator: String) extends Ops[Array[T]](separator):
  final def toString(values: Array[T]): String = 
    arrayMkString(arrayMap(values, _.toString), "[", ", ", "]")

  final override protected def toStrings(value: Array[T]): Array[String] =
    value.map(ops.write)

  final override protected def fromStrings(strings: Array[String]): Array[T] =
    arrayMap(strings, ops.read)
