package org.podval.tools.testing

import ForClass.*

object ScalaOnlyFixture extends Fixture(
  framework = org.podval.tools.testing.framework.ScalaTest,
  testSources = Seq(
    SourceFile("ExampleSpec",
      s"""import org.scalatest.{Tag, TagAnnotation}
         |import org.scalatest.flatspec.AnyFlatSpec
         |import org.scalatest.tagobjects.Slow
         |
         |@Tags.RequiresDb
         |class ExampleSpec extends AnyFlatSpec {
         |
         |  "The Scala language" must "add correctly" taggedAs(Slow) in {
         |    val sum = 1 + 1
         |    assert(sum === 2)
         |  }
         |
         |  it must "subtract correctly" taggedAs(Slow, ExampleSpec.DbTest) in {
         |    val diff = 4 - 1
         |    assert(diff === 3)
         |  }
         |}
         |
         |object ExampleSpec:
         |  object DbTest extends Tag("com.mycompany.tags.DbTest")
         |
         |object Tags:
         |  import java.lang.annotation.{ElementType, Retention, RetentionPolicy, Target};
         |
         |  @TagAnnotation
         |  @Retention(RetentionPolicy.RUNTIME)
         |  @Target(Array(ElementType.METHOD, ElementType.TYPE))
         |  class RequiresDb extends scala.annotation.Annotation
         |
         |""".stripMargin
    ),
    SourceFile("Nested",
      s"""import org.scalatest._
         |
         |class Nested extends Suites(
         |//  new ExampleSpec,
         |//  new StackSpec,
         |//  new TwoByTwoTest
         |)
         |
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
         |
         |""".stripMargin
    ),
    SourceFile("TwoByTwoTest",
      """import org.scalatest.flatspec.AnyFlatSpec
        |import org.scalatest.matchers.should.Matchers
        |
        |final class TwoByTwoTest extends AnyFlatSpec, Matchers:
        |
        |  "2*2 success" should "work" in {
        |    2*2 shouldBe 4
        |  }
        |
        |  "3*3 success" should "work" in {
        |    println("--- SCALA-ONLY TEST OUTPUT ---")
        |    3*3 shouldBe 9
        |  }
        |
        |  "2*2 failure" should "fail" in {
        |    2*2 shouldBe 5
        |  }
        |""".stripMargin
    )
  ),
  checks = Seq(
    forClass(className = "ExampleSpec",
      failedCount(0),
      skippedCount(0),
      passed("The Scala language must add correctly")
    ),
    forClass(className = "StackSpec",
      failedCount(0),
      skippedCount(1),
      passed("A Stack should pop values in last-in-first-out order"),
      skipped("A Stack should throw NoSuchElementException if an empty stack is popped")
    ),
    forClass(className = "TwoByTwoTest",
      failedCount(1),
      skippedCount(0),
      passed("2*2 success should work"),
      passed("3*3 success should work"),
      failed("2*2 failure should fail")
    )
  )
)
