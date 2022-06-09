package org.podval.tools.scalajs.testing

import sbt.testing.{Event as TEvent, Status as TStatus}

// Note: based on sbt.TestReportListener from org.scala-sbt.testing
abstract class TestEvent:
  def result: Option[TestResult]
  def detail: Seq[TEvent] = Nil

  override def toString: String = s"TestEvent($result, ${detail.map(toString)})"

  private def toString(event: TEvent): String =
    s"""Event(
       |  fullyQualifiedName=${event.fullyQualifiedName},
       |  fingerprint=${event.fingerprint},
       |  selector=${event.selector},
       |  status=${event.status},
       |  throwable=${event.throwable},
       |  duration=${event.duration}
       |)
       |""".stripMargin

object TestEvent:
  // TODO: de-dup
  given CanEqual[TStatus, TStatus] = CanEqual.derived

  def apply(events: Seq[TEvent]): TestEvent = new TestEvent:
    val result: Option[TestResult] = Some(overallResult(events))
    override val detail: Seq[TEvent] = events

  def overallResult(events: Seq[TEvent]): TestResult =
    events.foldLeft(TestResult.Passed: TestResult)((sum, event) => (sum, event.status) match
      case (TestResult.Error , _              ) => TestResult.Error
      case (_                , TStatus.Error  ) => TestResult.Error
      case (TestResult.Failed, _              ) => TestResult.Failed
      case (_                , TStatus.Failure) => TestResult.Failed
      case _                                    => TestResult.Passed
    )
