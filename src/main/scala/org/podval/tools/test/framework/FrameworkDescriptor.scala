package org.podval.tools.test.framework

import org.podval.tools.test.TestTagging

// Note: based on sbt.TestFramework from org.scala-sbt.testing
abstract class FrameworkDescriptor(
  val name: String,
  val implementationClassName: String
) derives CanEqual:

  def sharedPackages: List[String] = List(
    implementationClassName.substring(0, implementationClassName.lastIndexOf('.'))
  )

  def args(
    testTagging: TestTagging
  ): Array[String]

object FrameworkDescriptor:

  val all: List[FrameworkDescriptor] = List(
    ScalaTest,
    ScalaCheck,
    Specs2,
    JUnit4,
    JUnit5,
    MUnit,
    UTest
  )

  def forFramework(framework: sbt.testing.Framework): FrameworkDescriptor = forName(framework.name)

  def forName(name: String): FrameworkDescriptor = all
    .find(_.name == name)
    .getOrElse(throw IllegalArgumentException(s"Test framework descriptor for '$name' not found"))
