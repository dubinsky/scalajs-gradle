package org.podval.tools.testing

import ForClass.*

object ScalaTestFixture extends Fixture(
  framework = org.podval.tools.testing.framework.ScalaTest,
  testSources = Seq(
    SourceFile("ScalaTestTest" /*StringSpecification*/,
      s"""import org.scalatest.flatspec.AnyFlatSpec
         |import org.scalatest.matchers.should.Matchers
         |
         |final class ScalaTestTest extends AnyFlatSpec, Matchers:
         |  "2*2 success" should "pass" in { 2 * 2 shouldBe 4 }
         |  "2*2 failure" should "fail" in { 2 * 2 shouldBe 5 }
         |  ignore should "be ignored" in { 2 * 2 shouldBe 5 }
         |  "slow" should "be excluded correctly" taggedAs(org.podval.tools.testing.ScalaTestTagDb) in { true shouldBe true }
         |""".stripMargin
    ),
    SourceFile("ScalaTestTagDb",
      s"""import org.scalatest.Tag
         |
         |object ScalaTestTagDb extends Tag("org.podval.tools.testing.tags.Db")
         |
         |""".stripMargin
    ),
    SourceFile("ScalaTestNested",
      s"""import org.scalatest._
         |
         |class ScalaTestNested extends Suites(
         |  new ScalaTestTest,
         |  //  new StackSpec,
         |  //  new TwoByTwoTest
         |)
         |""".stripMargin
    )
  ),
  checks = Seq(forClass(className = "ScalaTestTest",
    failedCount(1),
    skippedCount(1),
    passed("2*2 success should pass"),
    failed("2*2 failure should fail"),
    skipped("2*2 failure should be ignored")
  ))
)

