package org.podval.tools.test

import sbt.testing.{Fingerprint, Framework}

// Note: based on org.scalajs.testing.adapter.TestAdapter
abstract class TestEnvironment(
  val testClassLoader: ClassLoader,
  val sourceMapper: Option[SourceMapper]
):
  final def loadAllFrameworks: List[Framework] = loadFrameworks(TestEnvironment.FrameworkDescriptor.values.toList)

  def loadFrameworks(descriptors: List[TestEnvironment.FrameworkDescriptor]): List[Framework]

  def close(): Unit

object TestEnvironment:
  // Note: based on sbt.TestFramework from org.scala-sbt.testing

  // TODO add whatever other test frameworks are supported by ScalaJS
  //  uTest
  //  MiniTest
  //  Greenlight
  //  Scala Test-State
  //  AirSpec
  //
  //  Nyaya
  //  Scalaprops
  enum FrameworkDescriptor(val implClassNames: String*) derives CanEqual:
    case ScalaCheck extends FrameworkDescriptor( //
      "org.scalacheck.ScalaCheckFramework"
    )
    case ScalaTest extends FrameworkDescriptor( //
      "org.scalatest.tools.Framework" //,
      // not an SBT framework: "org.scalatest.tools.ScalaTestFramework"
    )
    case Specs extends FrameworkDescriptor(
      "org.specs.runner.SpecsFramework"
    )
    case Specs2 extends FrameworkDescriptor(
      "org.specs2.runner.Specs2Framework",
      "org.specs2.runner.SpecsFramework"
    )
    case JUnit extends FrameworkDescriptor( //
      "com.novocode.junit.JUnitFramework"
    )
    case MUnit extends FrameworkDescriptor(
      "munit.Framework"
    )
