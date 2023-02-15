package org.podval.tools.test.framework

import org.podval.tools.test.TestTagsFilter
import sbt.testing.Framework

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

  private def newInstance: Any = Class.forName(implementationClassName).getConstructor().newInstance()

  def instantiate: Framework = newInstance.asInstanceOf[Framework]

  def maybeInstantiate: Option[Framework] =
    try newInstance match
      case framework: Framework => Some(framework)
      case other =>
        println(s"--- ${other.getClass.getName} is not an SBT framework")
        None
    catch
      case _: ClassNotFoundException => None

object FrameworkDescriptor:

  val all: List[FrameworkDescriptor] = List(
    ScalaTest,
    ScalaCheck,
    Specs2,
    JUnit4,
//    JUnit5,
    MUnit,
    UTest
  )

  def forFramework(framework: sbt.testing.Framework): FrameworkDescriptor = forName(framework.name)

  def forName(name: String): FrameworkDescriptor = all
    .find(_.name == name)
    .getOrElse(throw IllegalArgumentException(s"Test framework descriptor for '$name' not found"))
