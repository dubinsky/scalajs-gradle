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
       |  pureTest("non-effectful (pure) test"){
       |    expect("hello".size == 6)
       |  }
       |
       |  private val random = IO(java.util.UUID.randomUUID())
       |
       |  test("test with side-effects") {
       |    for {
       |      x <- random
       |      y <- random
       |    } yield expect(x != y)
       |  }
       |
       |  loggedTest("test with side-effects and a logger"){ log =>
       |    for {
       |      x <- random
       |      _ <- log.info(s"x : $$x")
       |      y <- random
       |      _ <- log.info(s"y : $$y")
       |    } yield expect(x != y)
       |  }
       |}
       |""".stripMargin
  ))
):
  override def checks(feature: Feature): Seq[ForClass] = Seq(
    forClass("WeaverTestTest",
      failedCount(1),
      skippedCount(0),
      failed("non-effectful (pure) test"),
      passed("test with side-effects"),
      passed("test with side-effects and a logger")
    )
  )
