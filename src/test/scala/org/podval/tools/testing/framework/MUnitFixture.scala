package org.podval.tools.testing.framework

import org.podval.tools.testing.testproject.ForClass.*
import org.podval.tools.testing.testproject.{Feature, Fixture, ForClass, SourceFile}

object MUnitFixture extends Fixture(
  framework = org.podval.tools.testing.framework.MUnit,
  testSources = Seq(SourceFile("MUnitTest",
    s"""class MUnitTest extends munit.FunSuite:
       |  val include = new munit.Tag("org.podval.tools.testing.IncludedTest")
       |  val exclude = new munit.Tag("org.podval.tools.testing.ExcludedTest")
       |
       |  test("successNotIncluded") { assertEquals(2, 2) }
       |  test("success".tag(include)) { assertEquals(2, 2) }
       |  test("failure".tag(include)) { assertEquals(42, 43) }
       |  test("excluded".tag(include).tag(exclude)) {}
       |""".stripMargin
  ))
):
  override def checks(feature: Feature): Seq[ForClass] = feature match
    case FrameworksTest.basicFunctionality =>
      Seq(
        forClass("MUnitTest",
          passed("successNotIncluded"),
          passed("success"),
          failed("failure"),
          absent("excluded"),

          testCount(3),
          failedCount(1),
          skippedCount(0),
        )
      )
    case FrameworksTest.withTagInclusions =>
      Seq(
        forClass("MUnitTest",
          absent("successNotIncluded"),
          passed("success"),
          failed("failure"),
          absent("excluded"),

          testCount(2),
          failedCount(1),
          skippedCount(0)
        )
      )

