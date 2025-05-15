package org.podval.tools.test.testproject

object TestTask:
  def testTask(
    includeTestNames: Seq[String],
    excludeTestNames: Seq[String],
    includeTags: Seq[String],
    excludeTags: Seq[String],
    maxParallelForks: Int
  ): String =
    s"""test {
       |  filter {
       |${includeTestNames.map(name => s"    includeTestsMatching '$name'").mkString("\n")}
       |${excludeTestNames.map(name => s"    excludeTestsMatching '$name'").mkString("\n")}
       |  }
       |  testLogging.lifecycle {
       |    events("STARTED", "PASSED", "SKIPPED", "FAILED", "STANDARD_OUT", "STANDARD_ERROR")
       |  }
       |  useSbt {
       |    includeCategories = ${includeTags.map(string => s"'$string'").mkString("[", ", ", "]")}
       |    excludeCategories = ${excludeTags.map(string => s"'$string'").mkString("[", ", ", "]")}
       |  }
       |  maxParallelForks = $maxParallelForks
       |}
       |""".stripMargin

