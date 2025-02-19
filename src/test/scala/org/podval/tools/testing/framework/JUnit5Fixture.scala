package org.podval.tools.testing.framework

import org.podval.tools.testing.testproject.ForClass.*
import org.podval.tools.testing.testproject.{Feature, Fixture, ForClass, SourceFile}

object JUnit5Fixture extends Fixture(
  framework = org.podval.tools.testing.framework.JUnit5,
  testSources = Seq(SourceFile("JUnit5Test",
    s"""import org.junit.jupiter.api.Assertions.fail
       |import org.junit.jupiter.api.Assumptions.assumeTrue
       |import org.junit.jupiter.api.{AfterAll, AfterEach, BeforeAll, BeforeEach, Disabled, Test}
       |
       |@Test
       |final class JUnit5Test:
       |  @BeforeAll def initAll(): Unit = ()
       |  @AfterAll def tearDownAll(): Unit = ()
       |  @BeforeEach def init(): Unit = ()
       |  @AfterEach def tearDown(): Unit = ()
       |
       |  @Test def succeedingTest(): Unit = ()
       |  @Test def failingTest(): Unit = fail("a failing test")
       |
       |  @Test @Disabled("for demonstration purposes") def skippedTest(): Unit =
       |    // not executed
       |    ()
       |
       |  @Test def abortedTest(): Unit =
       |    assumeTrue("abc".contains("Z"))
       |    fail("test should have been aborted")
       |
       |""".stripMargin
  ))
):
  override def checks(feature: Feature): Seq[ForClass] = Seq(
    forClass("JUnit5Test", 
      presentClass
    )
  )
