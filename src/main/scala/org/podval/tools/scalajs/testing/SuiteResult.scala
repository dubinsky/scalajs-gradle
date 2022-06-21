package org.podval.tools.scalajs.testing

import org.gradle.api.tasks.testing.TestResult.ResultType

// Note: based on sbt.TestReportListener from org.scala-sbt.testing
/** Provides the overall `result` of a group of tests (a suite) and test counts for each result type. */
final class SuiteResult(
  val result: ResultType,
  val successCount: Int,
  val failureCount: Int,
  val skippedCount: Int,
  val throwables: Seq[Throwable]
):

  // TODO rename
  def add(other: SuiteResult): SuiteResult = new SuiteResult(
    result = Util.max(result, other.result),
    successCount = successCount + other.successCount,
    failureCount = failureCount + other.failureCount,
    skippedCount = skippedCount + other.skippedCount,
    throwables = throwables ++ other.throwables
  )

  // Tests: succeeded 2, failed 0, canceled 0, ignored 0, pending 0
  override def toString: String =
    s"""SuiteResult(
       |  result=$result,
       |  successCount=$successCount,
       |  failureCount=$failureCount,
       |  skippedCount=$skippedCount,
       |  throwables=$throwables
       |)
       |""".stripMargin

object SuiteResult:

  /** Computes the overall result and counts for a suite with individual test results in `events`. */
  def apply(events: Seq[TestEvent]): SuiteResult =
    def count(status: ResultType): Int =
      import Util.given
      events.count(_.status == status)

    new SuiteResult(
      result = overallResult(events),
      successCount = count(ResultType.SUCCESS),
      failureCount = count(ResultType.FAILURE),
      skippedCount = count(ResultType.SKIPPED),
      throwables = events.collect { case e if e.throwable.isDefined => e.throwable.get }
    )

  def overallResult(events: Seq[TestEvent]): ResultType =
    events.foldLeft(ResultType.SUCCESS: ResultType)((sum: ResultType, event: TestEvent) => Util.max(sum, event.status))

  val Error: SuiteResult = new SuiteResult(ResultType.FAILURE, 0, 0, 0, Nil)
  val Empty: SuiteResult = new SuiteResult(ResultType.SUCCESS, 0, 0, 0, Nil)
