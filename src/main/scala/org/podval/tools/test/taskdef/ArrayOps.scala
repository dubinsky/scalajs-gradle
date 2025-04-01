package org.podval.tools.test.taskdef

import org.podval.tools.util.Scala212Collections.{arrayMap, arrayMkString}
import scala.reflect.ClassTag

class ArrayOps[T: ClassTag](ops: Ops[T], separator: String) extends Ops[Array[T]](separator):
  final def toString(values: Array[T]): String = 
    arrayMkString(arrayMap(values, _.toString), "[", ", ", "]")

  final override protected def toStrings(value: Array[T]): Array[String] =
    arrayMap(value, ops.write)

  final override protected def fromStrings(strings: Array[String]): Array[T] =
    // Now that we split in Ops.read() with "-1",
    // empty string array turns into an array with one empty string in it...
    if strings.length == 1 && strings(0).length == 0
    then Array.empty
    else arrayMap(strings, ops.read)
