package org.podval.tools.testing

import ForClass.*

object MUnitFixture extends Fixture(
  framework = org.podval.tools.testing.framework.MUnit,
  testSources = Seq(SourceFile("MUnitTest",
    s"""class MUnitTest extends munit.FunSuite:
       |  test("42 != 43") { assertEquals(42, 43) }
       |  test("2=2") { assertEquals(2, 2) }
       |""".stripMargin
  )),
  checks = Seq(forClass(className = "MUnitTest",
    failedCount(1),
    skippedCount(0),
    failed("42 != 43"),
    passed("2=2")
  ))
)
