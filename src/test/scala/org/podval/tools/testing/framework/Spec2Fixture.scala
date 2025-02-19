package org.podval.tools.testing.framework

import org.podval.tools.testing.testproject.ForClass.*
import org.podval.tools.testing.testproject.{Feature, Fixture, ForClass, SourceFile}

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
       |    start with 'Hello' $$e2
       |    end with 'world' $$e3 $${tag("org.podval.tools.testing.IncludedTest")}
       |    fail to end with 'xxx' $$e4 $${tag("org.podval.tools.testing.IncludedTest")}
       |    exclude tests tagged for exclusion $$e0 $${tag(
       |      "org.podval.tools.testing.IncludedTest",
       |      "org.podval.tools.testing.ExcludedTest"
       |    )}
       |    contain 11 characters $$e1
       |    
       |    \"\"\"
       |
       |    def e0 = "Hello world" must haveSize(0)
       |    def e1 = "Hello world" must haveSize(11)
       |    def e2 = "Hello world" must startWith("Hello")
       |    def e3 = "Hello world" must endWith("world")
       |    def e4 = "Hello world" must endWith("xxx")
       |""".stripMargin
  ))
):
  override def checks(feature: Feature): Seq[ForClass] = feature match
    case FrameworksTest.basicFunctionality => 
      Seq(
        forClass("Spec2Test",
          passed("start with 'Hello'"),
          passed("end with 'world'"),
          failed("fail to end with 'xxx'"),
          absent("exclude tests tagged for exclusion"),
          
          passed("contain 11 characters"),
    
          testCount(4),
          failedCount(1),
          skippedCount(0)
        )
      )
    case FrameworksTest.withTagInclusions =>
      Seq(
        forClass("Spec2Test",
          absent("start with 'Hello'"),
          passed("end with 'world'"),
          failed("fail to end with 'xxx'"),
          absent("exclude tests tagged for exclusion"),

          absent("contain 11 characters"),

          testCount(2),
          failedCount(1),
          skippedCount(0)
        )
      )
      

