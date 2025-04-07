package org.podval.tools.test.framework

import org.podval.tools.test.testproject.ForClass.*
import org.podval.tools.test.testproject.{Feature, Fixture, ForClass, SourceFile}

object ZioTestFixture extends Fixture(
  framework = org.podval.tools.test.framework.ZioTest,
  testSources = Seq(SourceFile("ZIOTestTest",
    s"""import zio.test._
       |
       |object ZIOTestTest extends ZIOSpecDefault {
       |  override def spec: Spec[TestEnvironment, Any] = suite("ZIOTestTest")(
       |    test("successNotIncluded") { assertTrue(1 == 1) },
       |    test("success") { assert(1)(Assertion.equalTo(1)) } @@ TestAspect.tag("org.podval.tools.test.IncludedTest"),
       |    test("failure") { assert(1)(Assertion.equalTo(2)) } @@ TestAspect.tag("org.podval.tools.test.IncludedTest"),
       |    test("excluded") { assertTrue(1 == 0) } @@ TestAspect.tag(
       |      "org.podval.tools.test.IncludedTest",
       |      "org.podval.tools.test.ExcludedTest"
       |    ),
       |    test("ignored") { val one = 1; assertTrue(one == 2) } @@ TestAspect.ignore @@ TestAspect.tag("org.podval.tools.test.IncludedTest"),
       |    test("assumption") { assertTrue(1 == 0) } @@ TestAspect.ifProp("property")(string => false)
       |  )
       |}
       |""".stripMargin
  ))
):
  override def checks(feature: Feature): Seq[ForClass] = feature match
    case FrameworksTest.basicFunctionality =>
      Seq(
        forClass(className = "ZIOTestTest",
          passed("ZIOTestTest - successNotIncluded"),
          passed("ZIOTestTest - success"),
          failed("ZIOTestTest - failure"),
          absent("ZIOTestTest - excluded"),
          skipped("ZIOTestTest - ignored"),
          skipped("ZIOTestTest - assumption"),

          testCount(5),
          failedCount(1),
          skippedCount(2),
        )
      )
    case FrameworksTest.withTagInclusions =>
      Seq(
        forClass(className = "ZIOTestTest",
          absent("ZIOTestTest - successNotIncluded"),
          passed("ZIOTestTest - success"),
          failed("ZIOTestTest - failure"),
          absent("ZIOTestTest - excluded"),
          skipped("ZIOTestTest - ignored"),
          absent("ZIOTestTest - assumption"),

          testCount(3),
          failedCount(1),
          skippedCount(1),
        )
      )
