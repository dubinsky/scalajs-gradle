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
         |  ignore should "be ignored" in { 2 * 2 shouldBe 5 }
         |
         |  "The Scala language" must "add correctly" in {
         |    val sum = 1 + 1
         |    assert(sum === 2)
         |  }
         |}
         |""".stripMargin
    ),
    SourceFile("StackSpec",
      s"""import org.scalatest.flatspec.AnyFlatSpec
         |import scala.collection.mutable
         |
         |class StackSpec extends AnyFlatSpec {
         |  "A Stack" should "pop values in last-in-first-out order" in {
         |    val stack = new mutable.Stack[Int]
         |    stack.push(1)
         |    stack.push(2)
         |    assert(stack.pop() === 2)
         |    assert(stack.pop() === 1)
         |  }
         |
         |  ignore should "throw NoSuchElementException if an empty stack is popped" in {
         |    val emptyStack = new mutable.Stack[String]
         |    intercept[NoSuchElementException] {
         |      emptyStack.pop()
         |    }
         |  }
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
          passed("The Scala language must add correctly"),
    
          testCount(5),
          failedCount(1),
          skippedCount(1)
        ),
        forClass("StackSpec",
          failedCount(0),
          skippedCount(1),
          passed("A Stack should pop values in last-in-first-out order"),
          skipped("A Stack should throw NoSuchElementException if an empty stack is popped")
        )
      )
    case FrameworksTest.withTagInclusions =>
      Seq(
        forClass("ScalaTestTest",
          absent("successNotIncluded should pass"),
          passed("success should pass"),
          failed("failure should fail"),
          absent("excluded should not run"),

          absent("excluded should be ignored"),
          absent("The Scala language must add correctly"),

          testCount(2),
          failedCount(1),
          skippedCount(0)
        )
      )
      


