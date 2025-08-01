package org.podval.tools.test.framework

import org.podval.tools.test.testproject.ForClass.*
import org.podval.tools.test.testproject.{Feature, Fixture, ForClass, SourceFile}

object HedgehogFixture extends Fixture(
  framework = org.podval.tools.test.framework.Hedgehog,
  testSources = Seq(SourceFile("HedgehogTest",
    s"""import hedgehog._
       |import hedgehog.runner._
       |
       |object HedgehogTest extends Properties {
       |  def tests: List[Test] =
       |    List(
       |      property("sq", sq),
       |      property("reverse", testReverse)
       |    )
       |
       |  def sq: Property = for {
       |    n <- Gen.int(Range.linear(0, 1)).forAll
       |  } yield 1 ==== 2
       |
       |  def testReverse: Property =
       |    for {
       |      xs <- Gen.alpha.list(Range.linear(0, 2)).forAll
       |    } yield xs.reverse.reverse ==== xs
       |}
       |""".stripMargin
  ))
):
  override def checks(feature: Feature): Seq[ForClass] = Seq(
    forClass("HedgehogTest",
      failedCount(1),
      skippedCount(0),
      failed("sq"),
      passed("reverse")
    )
  )
