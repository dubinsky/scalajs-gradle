package org.podval.tools.testing.framework

import org.podval.tools.testing.worker.TestTagsFilter

// Note: based on sbt.TestFramework from org.scala-sbt.testing
// TODO [frameworks] document (and auto-add) Framework implementations
// that are separate from the test frameworks (JUnit 4)
abstract class FrameworkDescriptor(
  val name: String,
  val implementationClassName: String
) derives CanEqual:

  def sharedPackages: List[String] = List(
    implementationClassName.substring(0, implementationClassName.lastIndexOf('.'))
  )

  def args(
    testTagsFilter: TestTagsFilter
  ): Array[String]

  def newInstance: AnyRef = Class.forName(implementationClassName)
    .getDeclaredConstructor()
    .newInstance()

object FrameworkDescriptor:

  // TODO [frameworks] split into Scala/ScalaJS
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

  def apply(name: String): FrameworkDescriptor = all
    .find(_.name == name)
    .getOrElse(throw IllegalArgumentException(s"Test framework descriptor for '$name' not found"))
