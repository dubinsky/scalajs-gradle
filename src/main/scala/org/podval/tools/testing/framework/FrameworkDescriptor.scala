package org.podval.tools.testing.framework

import org.podval.tools.testing.worker.TestTagsFilter
import sbt.testing.Framework
import java.io.File

// Note: based on sbt.TestFramework from org.scala-sbt.testing
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

  def instantiate: Framework = newInstance.asInstanceOf[Framework]

  def newInstance: AnyRef = Class.forName(
      implementationClassName,
      true,
      getClass.getClassLoader
    )
    .getConstructor()
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

  def apply(name: String): FrameworkDescriptor = all
    .find(_.name == name)
    .getOrElse(throw IllegalArgumentException(s"Test framework descriptor for '$name' not found"))
