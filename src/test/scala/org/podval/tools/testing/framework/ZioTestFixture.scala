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
       |    test("successNotIncluded") { assertTrue(1 == 1) },
       |    test("success") { assert(1)(Assertion.equalTo(1)) } @@ TestAspect.tag("org.podval.tools.testing.IncludedTest"),
       |    test("failure") { assert(1)(Assertion.equalTo(2)) } @@ TestAspect.tag("org.podval.tools.testing.IncludedTest"),
       |    test("excluded test") { assertTrue(1 == 0) } @@ TestAspect.tag(
       |      "org.podval.tools.testing.IncludedTest",
       |      "org.podval.tools.testing.ExcludedTest"
       |    ),
       |    test("failing test assertTrue") { val one = 1; assertTrue(one == 2) },
       |  )
       |""".stripMargin
  ))
):
  override def checks(feature: Feature): Seq[ForClass] = feature match
    case FrameworksTest.basicFunctionality =>
      Seq(
        forClass(className = "ZIOTestTest",
          passed("some suite - successNotIncluded"),
          passed("some suite - success"),
          failed("some suite - failure"),
          absent("some suite - excluded"),
    
          failed("some suite - failing test assertTrue"),
    
          testCount(4),
          failedCount(2),
          skippedCount(0),
        )
      )
    case FrameworksTest.withTagInclusions =>
      Seq(
        forClass(className = "ZIOTestTest",
          absent("some suite - successNotIncluded"),
          passed("some suite - success"),
          failed("some suite - failure"),
          absent("some suite - excluded"),

          absent("some suite - failing test assertTrue"),

          testCount(2),
          failedCount(1),
          skippedCount(0),
        )
      )


