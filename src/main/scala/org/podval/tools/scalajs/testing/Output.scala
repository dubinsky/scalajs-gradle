package org.podval.tools.scalajs.testing

import org.gradle.api.tasks.testing.TestResult.ResultType

// Note: based on sbt.Tests from org.scala-sbt.actions
/**
 * The result of a test run.
 *
 * @param overall The overall result of execution across all tests for all test frameworks in this test run.
 * @param events The result of each test group (suite) executed during this test run.
 * @param summaries Explicit summaries directly provided by test frameworks.  This may be empty, in which case a default summary will be generated.
 */
final case class Output(
  overall: ResultType,
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

object Output:
  val empty: Output = Output(
    overall = ResultType.SUCCESS,
    events = Map.empty,
    summaries = Seq.empty
  )

  def processResults(events: Map[String, SuiteResult]): Output = Output(
    overall = events
      .toSeq
      .map(_._2.result)
      .foldLeft[ResultType](ResultType.SUCCESS)(Util.max),
    events = events,
    summaries = Seq.empty
  )
