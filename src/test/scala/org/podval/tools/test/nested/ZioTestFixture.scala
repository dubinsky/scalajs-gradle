package org.podval.tools.test.nested

import org.podval.tools.test.testproject.ForClass.*
import org.podval.tools.test.testproject.{Feature, Fixture, ForClass, SourceFile}

object ZioTestFixture extends Fixture(
  framework = org.podval.tools.test.framework.ZioTest,
  includeTestNames = Seq("org.podval.tools.test.ZIOTestNesting"),
  testSources = Seq(
    SourceFile("ZIOTestNesting",
      s"""import zio.test._
         |
         |object ZIOTestNesting extends ZIOSpecDefault {
         |  override def spec: Spec[TestEnvironment, Any] = suite("ZIOTestNesting")(
         |    ZIOTestNested.spec
         |  )
         |}
         |""".stripMargin
    ),
    SourceFile("ZIOTestNested",
      s"""import zio.test._
         |
         |object ZIOTestNested extends ZIOSpecDefault {
         |  override def spec: Spec[TestEnvironment, Any] = suite("ZIOTestNested")(
         |    test("success") { assertTrue(1 == 1) },
         |    test("failure") { assertTrue(1 == 0) }
         |  )
         |}
         |""".stripMargin
    )
  )
):
  override def checks(feature: Feature): Seq[ForClass] = feature match
    case NestedSuitesTest.nestedSuites =>
      Seq(
        forClass("ZIOTestNesting",
          // nested test cases are attributed to the nesting suite
          passed("ZIOTestNesting - ZIOTestNested - success"),
          failed("ZIOTestNesting - ZIOTestNested - failure"),
          testCount(2),
          failedCount(1)
        ),
        forClass("ZIOTestNested",
          absentClass
        )
      )
      