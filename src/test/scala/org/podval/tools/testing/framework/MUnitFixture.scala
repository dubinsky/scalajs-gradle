package org.podval.tools.testing.framework

import org.podval.tools.testing.testproject.ForClass.*
import org.podval.tools.testing.testproject.{Feature, Fixture, ForClass, SourceFile}

object MUnitFixture extends Fixture(
  framework = org.podval.tools.testing.framework.MUnit,
  testSources = Seq(SourceFile("MUnitTest",
    s"""class MUnitTest extends munit.FunSuite:
       |  val exclude = new munit.Tag("org.podval.tools.testing.ExcludedTest")
       |
       |  test("excluded".tag(exclude)) {}
       |  test("42 != 43") { assertEquals(42, 43) }
       |  test("2=2") { assertEquals(2, 2) }
       |""".stripMargin
  ))
):
  override def checks(feature: Feature): Seq[ForClass] = Seq(
    forClass(className = "MUnitTest",
      absent("excluded"),
      failedCount(1),
      skippedCount(0),
      failed("42 != 43"),
      passed("2=2")
    )
  )

