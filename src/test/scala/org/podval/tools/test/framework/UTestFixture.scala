package org.podval.tools.test.framework

import org.podval.tools.test.testproject.ForClass.*
import org.podval.tools.test.testproject.{Feature, Fixture, ForClass, SourceFile}

object UTestFixture extends Fixture(
  framework = org.podval.tools.test.framework.UTest,
  testSources = Seq(SourceFile("UTestTest",
    s"""import utest._
       |
       |object UTestTest /* HelloTests */ extends TestSuite:
       |  val tests: Tests = Tests {
       |    test("test1") { throw new Exception("test1") }
       |    test("test2") { 1 }
       |    test("test3") { val a = List[Byte](1, 2); a(10) }
       |  }
       |""".stripMargin
  ))
):
  override def checks(feature: Feature): Seq[ForClass] = Seq(
    forClass("UTestTest",
      failedCount(2),
      skippedCount(0),
      failed("test1"),
      passed("test2"),
      failed("test3")
    )
  )

