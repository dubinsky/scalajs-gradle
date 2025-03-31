package org.podval.tools.test.nested

import org.podval.tools.test.testproject.ForClass.*
import org.podval.tools.test.testproject.{Feature, Fixture, ForClass, SourceFile}

object JUnit4Fixture extends Fixture(
  framework = org.podval.tools.test.framework.JUnit4,
  includeTestNames = Seq("org.podval.tools.test.JUnit4Nesting"),
  testSources = Seq(
    SourceFile("JUnit4Nesting",
      s"""import org.junit.runner.RunWith
         |import org.junit.runners.Suite
         |
         |@RunWith(classOf[Suite])
         |@Suite.SuiteClasses(Array(
         |  classOf[JUnit4Nested]
         |))
         |class JUnit4Nesting {
         |}
         |""".stripMargin
    ),
    SourceFile("JUnit4Nested",
      s"""import org.junit.Test
         |import org.junit.Assert.assertTrue
         |
         |final class JUnit4Nested {
         |  @Test def success(): Unit = assertTrue("should be true", true)
         |  @Test def failure(): Unit = assertTrue("should be true", false)
         |}
         |""".stripMargin
    )
  )
):
  override def checks(feature: Feature): Seq[ForClass] = feature match
    case NestedSuitesTest.nestedSuites =>
      Seq(
        forClass("JUnit4Nesting",
          // nested test cases are attributed to the nested suite
          testCount(0)
        ),
        forClass("JUnit4Nested",
          passed("success"),
          testCount(2),
          failedCount(1)
        )
      )
