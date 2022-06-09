package org.podval.tools.scalajs.testing

// Note: based on sbt.Tests from org.scala-sbt.actions
/**
 * The result of a test run.
 *
 * @param overall The overall result of execution across all tests for all test frameworks in this test run.
 * @param events The result of each test group (suite) executed during this test run.
 * @param summaries Explicit summaries directly provided by test frameworks.  This may be empty, in which case a default summary will be generated.
 */
final case class Output(
  overall: TestResult,
  events: Map[String, SuiteResult],
  summaries: Iterable[Summary]
):
  override def toString: String =
    s"""
       |Output(
       |  overall=$overall,
       |  events=${events.mkString(", ")}
       |  summaries=${summaries.mkString(", ")}
       |)
       |""".stripMargin
