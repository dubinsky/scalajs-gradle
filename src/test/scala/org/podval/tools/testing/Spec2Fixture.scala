package org.podval.tools.testing

import ForClass.*

object Spec2Fixture extends Fixture(
  framework = org.podval.tools.testing.framework.Specs2,
  testSources = Seq(SourceFile("Spec2Test",
    s"""import org.specs2.*
       |
       |final class Spec2Test extends Specification:
       |  def is = s2\"\"\"
       |
       |  This is a specification to check the 'Hello world' string
       |
       |  The 'Hello world' string should
       |    exclude tests tagged for exclusion $$e0 $${tag("org.podval.tools.testing.ExcludedTest")}
       |    contain 11 characters $$e1
       |    start with 'Hello' $$e2
       |    end with 'world' $$e3
       |    fail to end with 'xxx' $$e4
       |
       |    \"\"\"
       |
       |    def e0 = "Hello world" must haveSize(0)
       |    def e1 = "Hello world" must haveSize(11)
       |    def e2 = "Hello world" must startWith("Hello")
       |    def e3 = "Hello world" must endWith("world")
       |    def e4 = "Hello world" must endWith("xxx")
       |""".stripMargin
  )),
  checks = Seq(forClass("Spec2Test",
    absent("exclude tests tagged for exclusion"),
    failedCount(1),
    skippedCount(0),
    passed("contain 11 characters"),
    passed("start with 'Hello'"),
    passed("end with 'world'"),
    failed("fail to end with 'xxx'")
  ))
)
