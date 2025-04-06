package org.podval.tools.test.framework

import org.podval.tools.test.testproject.ForClass.*
import org.podval.tools.test.testproject.{Feature, Fixture, ForClass, SourceFile}

object MUnitFixture extends Fixture(
  framework = org.podval.tools.test.framework.MUnit,
  testSources = Seq(SourceFile("MUnitTest",
    s"""class MUnitTest extends munit.FunSuite {
       |  val include = new munit.Tag("org.podval.tools.test.IncludedTest")
       |  val exclude = new munit.Tag("org.podval.tools.test.ExcludedTest")
       |
       |  test("successNotIncluded") { assertEquals(2, 2) }
       |  test("success".tag(include)) { assertEquals(2, 2) }
       |  test("failure".tag(include)) { assertEquals(42, 43) }
       |  test("excluded".tag(include).tag(exclude)) {}
       |  test("ignored".tag(include).ignore) {}
       |}
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
          skipped("ignored"),

          testCount(4),
          failedCount(1),
          skippedCount(1),
        )
      )
    case FrameworksTest.withTagInclusions =>
      Seq(
        forClass("MUnitTest",
          absent("successNotIncluded"),
          passed("success"),
          failed("failure"),
          absent("excluded"),
          skipped("ignored"),

          testCount(3),
          failedCount(1),
          skippedCount(1)
        )
      )

