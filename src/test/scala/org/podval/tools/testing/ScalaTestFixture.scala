package org.podval.tools.testing

import ForClass.*

object ScalaTestFixture extends Fixture(
  framework = org.podval.tools.testing.framework.ScalaTest,
  testSources = Seq(
    SourceFile("ScalaTestTest",
      s"""import org.scalatest.flatspec.AnyFlatSpec
         |import org.scalatest.matchers.should.Matchers
         |
         |final class ScalaTestTest extends AnyFlatSpec, Matchers:
         |  object ExcludedTest extends org.scalatest.Tag("org.podval.tools.testing.ExcludedTest")
         |
         |  "excluded" should "not run" taggedAs(ExcludedTest) in {  true shouldBe true }
         |  "2*2 success" should "pass" in { 2 * 2 shouldBe 4 }
         |  "2*2 failure" should "fail" in { 2 * 2 shouldBe 5 }
         |  ignore should "be ignored" in { 2 * 2 shouldBe 5 }
         |
         |  "The Scala language" must "add correctly" in {
         |    val sum = 1 + 1
         |    assert(sum === 2)
         |  }
         |""".stripMargin
    ),
    SourceFile("StackSpec",
      s"""import org.scalatest.flatspec.AnyFlatSpec
         |import scala.collection.mutable
         |
         |// Note: from the ScalaTest documentation
         |class StackSpec extends AnyFlatSpec {
         |
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
    ),
    SourceFile("ScalaTestNested",
      s"""import org.scalatest._
         |
         |class ScalaTestNested extends Suites(
         |  new ScalaTestTest,
         |  //  new StackSpec
         |)
         |""".stripMargin
    )
  ),
  checks = Seq(
    forClass(className = "ScalaTestTest",
      absent("excluded"),
      failedCount(1),
      skippedCount(1),
      passed("2*2 success should pass"),
      failed("2*2 failure should fail"),
      skipped("2*2 failure should be ignored"),
      passed("The Scala language must add correctly")
    ),
    forClass(className = "StackSpec",
      failedCount(0),
      skippedCount(1),
      passed("A Stack should pop values in last-in-first-out order"),
      skipped("A Stack should throw NoSuchElementException if an empty stack is popped")
    )
  )
)

