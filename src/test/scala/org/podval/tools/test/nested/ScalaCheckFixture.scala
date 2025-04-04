package org.podval.tools.test.nested

import org.podval.tools.test.testproject.ForClass.*
import org.podval.tools.test.testproject.{Feature, Fixture, ForClass, SourceFile}

object ScalaCheckFixture extends Fixture(
  framework = org.podval.tools.test.framework.ScalaCheck,
  includeTestNames = Seq("org.podval.tools.test.ScalaCheckNesting"),
  testSources = Seq(
    SourceFile("ScalaCheckNesting",
      s"""object ScalaCheckNesting extends org.scalacheck.Properties("ScalaCheckNesting") {
         |  include(ScalaCheckNested)
         |}
         |""".stripMargin
    ),
    SourceFile("ScalaCheckNested",
      s"""object ScalaCheckNested extends org.scalacheck.Properties("ScalaCheckNested") {
         |  property("success") = org.scalacheck.Prop.passed
         |  property("failure") = org.scalacheck.Prop.falsified
         |}
         |""".stripMargin
    )
  )
):
  override def checks(feature: Feature): Seq[ForClass] = feature match
    case NestedSuitesTest.nestedSuites =>
      Seq(
        forClass("ScalaCheckNesting",
          // nested test cases are incorrectly attributed to the nesting suite -
          // and there is nothing that can be done about it...
          // see https://github.com/typelevel/scalacheck/pull/1107
          passed("ScalaCheckNesting.ScalaCheckNested.success"),
          failed("ScalaCheckNesting.ScalaCheckNested.failure"),
          testCount(2),
          failedCount(1)
        ),
        forClass("ScalaCheckNested",
          absentClass
        )
      )



