package org.podval.tools.test.framework

import org.podval.tools.test.testproject.ForClass.*
import org.podval.tools.test.testproject.{Feature, Fixture, ForClass, SourceFile}

object WeaverTestFixture extends Fixture(
  framework = org.podval.tools.test.framework.WeaverTest,
  testSources = Seq(SourceFile("WeaverTestTest",
    s"""import weaver.SimpleIOSuite
       |import cats.effect._
       |
       |// Suites must be "objects" for them to be picked by the framework
       |object WeaverTestTest extends SimpleIOSuite {
       |
       |  pureTest("non-effectful (pure) success"){
       |    expect("hello".size == 5)
       |  }
       |
       |  pureTest("non-effectful (pure) failure"){
       |    expect("hello".size == 6)
       |  }
       |}
       |""".stripMargin
  ))
):
  override def checks(feature: Feature): Seq[ForClass] = Seq(
    forClass("WeaverTestTest",
      testCount(2),
      failedCount(1),
      skippedCount(0),
      passed("non-effectful (pure) success"),
      failed("non-effectful (pure) failure"),
    )
  )
