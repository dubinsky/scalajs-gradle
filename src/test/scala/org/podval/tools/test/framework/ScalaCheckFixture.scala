package org.podval.tools.test.framework

import org.podval.tools.test.testproject.ForClass.*
import org.podval.tools.test.testproject.{Feature, Fixture, ForClass, SourceFile}

object ScalaCheckFixture extends Fixture(
  org.podval.tools.test.framework.ScalaCheck,
  testSources = Seq(SourceFile("ScalaCheckTest",
    s"""import org.scalacheck.Properties
       |import org.scalacheck.Prop.forAll
       |
       |class ScalaCheckTest extends Properties("String") {
       |  property("startsWith") = forAll { (a: String, b: String) => (a+b).startsWith(a) }
       |  property("concatenate") = forAll { (a: String, b: String) => (a+b).length > a.length && (a+b).length > b.length }
       |  property("substring") = forAll { (a: String, b: String, c: String) => (a+b+c).substring(a.length, a.length+b.length) == b }
       |}
       |""".stripMargin
  ))
):
  override def checks(feature: Feature): Seq[ForClass] = Seq(
    forClass("ScalaCheckTest",
      failedCount(1),
      skippedCount(0),
      passed("String.startsWith"),
      failed("String.concatenate"),
      passed("String.substring")
    )
  )

