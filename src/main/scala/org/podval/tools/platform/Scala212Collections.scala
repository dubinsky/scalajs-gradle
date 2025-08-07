package org.podval.tools.platform

import scala.reflect.ClassTag

// Array operations that avoid issues with missing classes/methods while running on Scala 2.12.
object Scala212Collections:
  def arrayMap[A, B: ClassTag](array: Array[A], f: A => B): Array[B] =
    val result: Array[B] = new Array[B](array.length)
    var i: Int = 0; while i < array.length do { result(i) = f(array(i)); i = i + 1 }
    result

  def arrayFlatMap[A, B: ClassTag](array: Array[A], f: A => Array[B]): Array[B] =
    var result: Array[B] = Array.empty
    var i: Int = 0
    while i < array.length do { result = arrayConcat(result, f(array(i))); i = i + 1 }
    result

  def arrayForEach[A](array: Array[A], u: A => Unit): Unit =
    var i: Int = 0; while i < array.length do { u(array(i)); i = i + 1 }

  def arrayFind[A](array: Array[A], p: A => Boolean): Option[A] =
    var result: Option[A] = None
    var i: Int = 0
    while result.isEmpty && (i < array.length) do
      if p(array(i)) then result = Some(array(i))
      i = i + 1
    result
  
  def arrayConcat[A: ClassTag](left: Array[? <: A], right: Array[? <: A]): Array[A] =
    val result: Array[A] = new Array[A](left.length + right.length)
    var l: Int = 0; while l < left .length do { result(l) = left(l)               ; l = l + 1 }
    var r: Int = 0; while r < right.length do { result(left.length + r) = right(r); r = r + 1 }
    result

  def arrayAppend[A: ClassTag](array: Array[A], element: A): Array[A] =
    // val elementArray: Array[A] = Array(element) breaks on Scala 2.12, so:
    val elementArray: Array[A] = new Array[A](1)
    elementArray(0) = element
    arrayConcat(array, elementArray)

//  def arrayForAll[A](
//    array: Array[A],
//    p: A => Boolean
//  ): Boolean =
//    var result: Boolean = true
//    var i: Int = 0
//    while result && (i < array.length) do
//      result = p(array(i))
//      i = i + 1
//    result

  def arrayZipForAll[A](
    left: Array[A],
    right: Array[A],
    p: (A, A) => Boolean
  ): Boolean =
    var result: Boolean = left.length == right.length
    var i: Int = 0
    while result && (i < left.length) do
      result = p(left(i), right(i))
      i = i + 1
    result

  def arrayMkString(
    array: Array[String],
    separator: String
  ): String =
    var result: String = ""
    var i: Int = 0
    while i < array.length do
      if i > 0 then result = result.concat(separator)
      result = result.concat(array(i))
      i = i + 1
    result
