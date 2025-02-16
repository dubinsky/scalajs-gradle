package org.podval.tools.testing

import ForClass.*
import org.podval.tools.build.ScalaPlatform

object ZioTestFixture extends Fixture(
  framework = org.podval.tools.testing.framework.ZioTest,
  testSources = Seq(SourceFile("ZIOTestTest",
    s"""import zio.test.*
       |import zio.Scope
       |
       |object ZIOTestTest extends ZIOSpecDefault:
       |  override def spec: Spec[TestEnvironment & Scope, Any] = suite("some suite")(
       |    test("failing test") { assert(1)(Assertion.equalTo(2)) },
       |    test("passing test") { assert(1)(Assertion.equalTo(1)) },
       |    test("failing test assertTrue") { val one = 1; assertTrue(one == 2) },
       |    test("passing test assertTrue") { assertTrue(1 == 1) }
       |  )
       |""".stripMargin
  )),
  checks = Seq(forClass(className = "ZIOTestTest",
    failedCount(2),
    skippedCount(0),
    passed("some suite - passing test"),
    passed("some suite - passing test assertTrue"),
    failed("some suite - failing test"),
    failed("some suite - failing test assertTrue")
  ))
):
  // TODO I do not get test events when running ZioTest on Scala.js!
  override def works(feature: Feature, platform: ScalaPlatform): Boolean = !platform.backend.isJS

