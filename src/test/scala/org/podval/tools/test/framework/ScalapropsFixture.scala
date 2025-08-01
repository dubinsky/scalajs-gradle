package org.podval.tools.test.framework

import org.podval.tools.test.testproject.ForClass.*
import org.podval.tools.test.testproject.{Feature, Fixture, ForClass, SourceFile}

object ScalapropsFixture extends Fixture(
  org.podval.tools.test.framework.Scalaprops,
  testSources = Seq(SourceFile("ScalapropsTest",
    s"""import scalaprops._
       |
       |object ScalapropsTest extends Scalaprops {
       |  val revAndRevIsOriginal = Property.forAll { (xs: List[Int]) => xs.reverse.reverse == xs }
       |  val revIsOriginal = Property.forAll { (xs: List[Int]) => xs.reverse == xs }
       |  val empty = Property.forAll { (xs: List[Int]) => xs.isEmpty }.ignore("ignore a test")
       |}
       |""".stripMargin
  ))
):
  override def checks(feature: Feature): Seq[ForClass] = Seq(
    forClass("ScalapropsTest",
      failedCount(1),
      skippedCount(1),
      passed("revAndRevIsOriginal"),
      failed("revIsOriginal"),
      skipped("empty")
    )
  )

