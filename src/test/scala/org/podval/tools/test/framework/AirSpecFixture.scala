package org.podval.tools.test.framework

import org.podval.tools.test.testproject.ForClass.*
import org.podval.tools.test.testproject.{Feature, Fixture, ForClass, SourceFile}

object AirSpecFixture extends Fixture(
  framework = org.podval.tools.test.framework.AirSpec,
  testSources = Seq(SourceFile("AirSpecTest",
    s"""import wvlet.airspec._
       |
       |class AirSpecTest extends AirSpec {
       |  test("empty Seq size should be 0") { Seq.empty.size shouldBe 0 }
       |  test("square of 2 should be 4") { 2*2 shouldBe 4 }
       |  test("square of 2 should fail to be 5") { 2*2 shouldBe 5 }
       |
       |  test("Seq.empty.head should fail") {
       |    intercept[NoSuchElementException] {
       |      Seq.empty.head
       |    }
       |  }
       |}
       |""".stripMargin
  ))
):
  override def checks(feature: Feature): Seq[ForClass] = Seq(
    forClass("AirSpecTest",
      failedCount(1),
      skippedCount(0),
      failed("square of 2 should fail to be 5"),
      passed("empty Seq size should be 0"),
      passed("square of 2 should be 4")
    )
  )
