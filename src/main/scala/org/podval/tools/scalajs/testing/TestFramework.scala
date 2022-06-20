package org.podval.tools.scalajs.testing

// Note: based on sbt.TestFramework from org.scala-sbt.testing
final class TestFramework(val implClassNames: String*) derives CanEqual:
  override def equals(o: Any): Boolean = o.asInstanceOf[Matchable] match
    case x: TestFramework => (this.implClassNames.toList == x.implClassNames.toList)
    case _                => false

  override def hashCode: Int =
    37 * (17 + implClassNames.##) + "TestFramework".##

  override def toString: String =
    "TestFramework(" + implClassNames.mkString(", ") + ")"

// TODO add whatever other test frameworks are supported by ScalaJS
object TestFramework:
  val ScalaCheck: TestFramework = TestFramework(
    "org.scalacheck.ScalaCheckFramework"
  )
  val ScalaTest: TestFramework = TestFramework(
    "org.scalatest.tools.Framework",
    "org.scalatest.tools.ScalaTestFramework"
  )
  val Specs: TestFramework = TestFramework(
    "org.specs.runner.SpecsFramework"
  )
  val Specs2: TestFramework = TestFramework(
    "org.specs2.runner.Specs2Framework",
    "org.specs2.runner.SpecsFramework"
  )
  val JUnit: TestFramework = TestFramework(
    "com.novocode.junit.JUnitFramework"
  )
  val MUnit: TestFramework = TestFramework(
    "munit.Framework"
  )

  val all: List[TestFramework] = List(ScalaCheck, Specs2, Specs, ScalaTest, JUnit, MUnit)
