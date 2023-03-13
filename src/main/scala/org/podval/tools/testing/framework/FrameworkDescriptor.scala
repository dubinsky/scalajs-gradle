package org.podval.tools.testing.framework

import org.podval.tools.testing.worker.TestTagsFilter

// Note: based on sbt.TestFramework from org.scala-sbt.testing
abstract class FrameworkDescriptor(
  val name: String,
  val implementationClassName: String
) derives CanEqual:
  def isScalaSupported: Boolean = true
  def isScalaJSSupported: Boolean = true

  final def sharedPackages: List[String] = List(
    implementationClassName.substring(0, implementationClassName.lastIndexOf('.'))
  )

  def args(
    testTagsFilter: TestTagsFilter
  ): Seq[String]

  final def newInstance: AnyRef = Class.forName(implementationClassName)
    .getDeclaredConstructor()
    .newInstance()

object FrameworkDescriptor:

  val all: List[FrameworkDescriptor] = List(
    ScalaTest,
    ScalaCheck,
    Specs2,
    JUnit4,
////    JUnit5,
    MUnit,
    UTest,
    ZIOTest
  )

  def scalaSupported  : List[FrameworkDescriptor] = all.filter(_.isScalaSupported  )
  def scalaJSSupported: List[FrameworkDescriptor] = all.filter(_.isScalaJSSupported)

  def apply(name: String): FrameworkDescriptor = all
    .find(_.name == name)
    .getOrElse(throw IllegalArgumentException(s"Test framework descriptor for '$name' not found"))

  def listOption(name: String, values: Array[String]): Seq[String] =
    if values.isEmpty then Seq.empty else
      val valuesStr: String = values.mkString(",")
      Seq(s"$name=$valuesStr")

  def listOfOptions(name: String, values: Array[String]): Seq[String] =
    values.toIndexedSeq.flatMap((value: String) => Seq(name, value))
    