package org.podval.tools.testing.framework

import org.podval.tools.testing.testproject.ForClass.*
import org.podval.tools.testing.testproject.{Feature, Fixture, ForClass, SourceFile}

object ZioTestFixture extends Fixture(
  framework = org.podval.tools.testing.framework.ZioTest,
  testSources = Seq(SourceFile("ZIOTestTest",
    s"""import zio.test.*
       |import zio.Scope
       |
       |object ZIOTestTest extends ZIOSpecDefault:
       |  override def spec: Spec[TestEnvironment & Scope, Any] = suite("some suite")(
       |    test("excluded test") { assertTrue(1 == 0) } @@ TestAspect.tag(
       |      "org.podval.tools.testing.IncludedTest",
       |      "org.podval.tools.testing.ExcludedTest"
       |    ),
       |    test("failing test") { assert(1)(Assertion.equalTo(2)) },
       |    test("passing test") { assert(1)(Assertion.equalTo(1)) },
       |    test("failing test assertTrue") { val one = 1; assertTrue(one == 2) },
       |    test("passing test assertTrue") { assertTrue(1 == 1) }
       |  )
       |""".stripMargin
  ))
):
  override def checks(feature: Feature): Seq[ForClass] = Seq(
    forClass(className = "ZIOTestTest",
      absent("excluded test"),
      failedCount(2),
      skippedCount(0),
      passed("some suite - passing test"),
      passed("some suite - passing test assertTrue"),
      failed("some suite - failing test"),
      failed("some suite - failing test assertTrue")
    )
  )



