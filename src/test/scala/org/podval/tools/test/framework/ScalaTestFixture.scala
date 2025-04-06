package org.podval.tools.test.framework

import org.podval.tools.test.testproject.ForClass.*
import org.podval.tools.test.testproject.{Feature, Fixture, ForClass, SourceFile}

object ScalaTestFixture extends Fixture(
  framework = org.podval.tools.test.framework.ScalaTest,
  testSources = Seq(
    SourceFile("ScalaTestTest",
      s"""import org.scalatest.flatspec.AnyFlatSpec
         |import org.scalatest.matchers.should.Matchers
         |
         |final class ScalaTestTest extends AnyFlatSpec with Matchers {
         |  object Include extends org.scalatest.Tag("org.podval.tools.test.IncludedTest")
         |  object Exclude extends org.scalatest.Tag("org.podval.tools.test.ExcludedTest")
         |
         |  "successNotIncluded" should "pass" in { 2 * 2 shouldBe 4 }
         |  "success" should "pass" taggedAs(Include) in { 2 * 2 shouldBe 4 }
         |  "failure" should "fail" taggedAs(Include) in { 2 * 2 shouldBe 5 }
         |  "excluded" should "not run" taggedAs(Include, Exclude) in {  true shouldBe true }
         |  ignore should "be ignored" taggedAs(Include) in { 2 * 2 shouldBe 5 }
         |}
         |""".stripMargin
    )
  )
):
  override def checks(feature: Feature): Seq[ForClass] = feature match
    case FrameworksTest.basicFunctionality =>
      Seq(
        forClass("ScalaTestTest",
          passed("successNotIncluded should pass"),
          passed("success should pass"),
          failed("failure should fail"),
          absent("excluded should not run"),
          skipped("excluded should be ignored"),

          testCount(4),
          failedCount(1),
          skippedCount(1)
        )
      )
    case FrameworksTest.withTagInclusions =>
      Seq(
        forClass("ScalaTestTest",
          absent("successNotIncluded should pass"),
          passed("success should pass"),
          failed("failure should fail"),
          absent("excluded should not run"),
          skipped("excluded should be ignored"),

          testCount(3),
          failedCount(1),
          skippedCount(1)
        )
      )
      


