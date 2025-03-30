package org.podval.tools.test.nested

import org.podval.tools.test.testproject.ForClass.*
import org.podval.tools.test.testproject.{Feature, Fixture, ForClass, SourceFile}

object ScalaTestFixture extends Fixture(
  framework = org.podval.tools.test.framework.ScalaTest,
  includeTestNames = Seq("org.podval.tools.test.ScalaTestNesting"),
  testSources = Seq(
    SourceFile("ScalaTestNested",
      s"""import org.scalatest.flatspec.AnyFlatSpec
         |import org.scalatest.matchers.should.Matchers
         |
         |final class ScalaTestNested extends AnyFlatSpec with Matchers {
         |  "success" should "pass" in {}
         |  "failure" should "fail" in { 2 * 2 shouldBe 5 }
         |}
         |""".stripMargin
    ),
    SourceFile("ScalaTestNesting",
      s"""import org.scalatest.Suites
         |
         |class ScalaTestNesting extends Suites(
         |  new ScalaTestNested
         |)
         |""".stripMargin
    )
  )
):
  override def checks(feature: Feature): Seq[ForClass] = feature match
    case NestedSuitesTest.nestedSuites =>
      Seq(
        forClass("ScalaTestNesting",
          passed("success should pass"),
          testCount(2),
          failedCount(1)
        ),
        forClass("ScalaTestNested",
          absentClass
        )
      )



