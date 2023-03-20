package org.podval.tools.testing

import ForClass.*

object ScalaCheckFixture extends Fixture(
  org.podval.tools.testing.framework.ScalaCheck,
  testSources = Seq(SourceFile("ScalaCheckTest" /*StringSpecification*/,
    s"""import org.scalacheck.Properties
       |import org.scalacheck.Prop.forAll
       |
       |class ScalaCheckTest extends Properties("String") {
       |  property("startsWith") = forAll { (a: String, b: String) => (a+b).startsWith(a) }
       |  property("concatenate") = forAll { (a: String, b: String) => (a+b).length > a.length && (a+b).length > b.length }
       |  property("substring") = forAll { (a: String, b: String, c: String) => (a+b+c).substring(a.length, a.length+b.length) == b }
       |}
       |""".stripMargin
  )),
  checks = Seq(forClass("ScalaCheckTest",
    failedCount(1),
    skippedCount(0),
    passed("String.startsWith"),
    failed("String.concatenate"),
    passed("String.substring")
  ))
)
