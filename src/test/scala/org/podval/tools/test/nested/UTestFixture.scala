package org.podval.tools.test.nested

import org.podval.tools.test.testproject.ForClass.*
import org.podval.tools.test.testproject.{Feature, Fixture, ForClass, SourceFile}

object UTestFixture extends Fixture(
  framework = org.podval.tools.test.framework.UTest,
  includeTestNames = Seq("org.podval.tools.test.UTestNesting"),
  testSources = Seq(
    SourceFile("UTestNesting",
      s"""import utest._
         |
         |object UTestNesting extends TestSuite {
         |  val tests: Tests = Tests { 
         |    test("UTestNesting") {
         |      test("UTestNested") {
         |        test("success") { assert(1 == 1) }
         |        test("failure") { assert(1 == 0) }
         |      }
         |    }
         |  }
         |}
         |""".stripMargin
    )
  )
):
  override def checks(feature: Feature): Seq[ForClass] = feature match
    case NestedSuitesTest.nestedSuites =>
      Seq(
        forClass("UTestNesting",
          // nested test cases are attributed to the nesting suite
          passed("UTestNesting.UTestNested.success"),
          failed("UTestNesting.UTestNested.failure"),
          testCount(2),
          failedCount(1)
        ),
        forClass("UTestNested",
          absentClass
        )
      )
      