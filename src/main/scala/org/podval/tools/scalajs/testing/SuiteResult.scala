package org.podval.tools.scalajs.testing

import sbt.testing.{Event as TEvent, Status as TStatus}

// Note: based on sbt.TestReportListener from org.scala-sbt.testing
/** Provides the overall `result` of a group of tests (a suite) and test counts for each result type. */
final class SuiteResult(
  val result: TestResult,
  val passedCount: Int,
  val failureCount: Int,
  val errorCount: Int,
  val skippedCount: Int,
  val ignoredCount: Int,
  val canceledCount: Int,
  val pendingCount: Int,
  val throwables: Seq[Throwable]
):
  def this(
    result: TestResult,
    passedCount: Int,
    failureCount: Int,
    errorCount: Int,
    skippedCount: Int,
    ignoredCount: Int,
    canceledCount: Int,
    pendingCount: Int,
  ) =
    this(
      result,
      passedCount,
      failureCount,
      errorCount,
      skippedCount,
      ignoredCount,
      canceledCount,
      pendingCount,
      Nil
    )

  def +(other: SuiteResult): SuiteResult =
    val combinedTestResult: TestResult = (result, other.result) match
      case (TestResult.Passed, TestResult.Passed) => TestResult.Passed
      case (_                , TestResult.Error ) => TestResult.Error
      case (TestResult.Error , _                ) => TestResult.Error
      case _                                      => TestResult.Failed

    new SuiteResult(
      combinedTestResult,
      passedCount + other.passedCount,
      failureCount + other.failureCount,
      errorCount + other.errorCount,
      skippedCount + other.skippedCount,
      ignoredCount + other.ignoredCount,
      canceledCount + other.canceledCount,
      pendingCount + other.pendingCount,
      throwables ++ other.throwables
    )

  // Tests: succeeded 2, failed 0, canceled 0, ignored 0, pending 0
  override def toString: String =
    s"""SuiteResult(
       |  result=$result,
       |  passedCount=$passedCount,
       |  failureCount=$failureCount,
       |  errorCount=$errorCount,
       |  skippedCount=$skippedCount,
       |  ignoredCount=$ignoredCount,
       |  canceledCount=$canceledCount,
       |  pendingCount=$pendingCount,
       |  throwables=$throwables
       |)
       |""".stripMargin

object SuiteResult:

  // TODO: de-dup
  given CanEqual[TStatus, TStatus] = CanEqual.derived

  /** Computes the overall result and counts for a suite with individual test results in `events`. */
  def apply(events: Seq[TEvent]): SuiteResult =
    def count(status: TStatus): Int = events.count(_.status == status)
    new SuiteResult(
      TestEvent.overallResult(events),
      count(TStatus.Success),
      count(TStatus.Failure),
      count(TStatus.Error),
      count(TStatus.Skipped),
      count(TStatus.Ignored),
      count(TStatus.Canceled),
      count(TStatus.Pending),
      events.collect { case e if e.throwable.isDefined => e.throwable.get }
    )

  val Error: SuiteResult = new SuiteResult(TestResult.Error , 0, 0, 0, 0, 0, 0, 0)
  val Empty: SuiteResult = new SuiteResult(TestResult.Passed, 0, 0, 0, 0, 0, 0, 0)
