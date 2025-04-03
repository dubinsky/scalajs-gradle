package org.podval.tools.test.run

import org.podval.tools.util.Scala212Collections.arrayForEach
import sbt.testing.{Event, EventHandler, Fingerprint, Logger, NestedSuiteSelector, NestedTestSelector,
  OptionalThrowable, Selector, Status, SuiteSelector, Task, TaskDef, TestSelector, TestWildcardSelector}

final class DryRunSbtTask(
  override val taskDef: TaskDef
) extends Task:
  override def tags(): Array[String] = Array.empty

  override def execute(
    eventHandler: EventHandler,
    loggers: Array[Logger]
  ): Array[Task] =
    arrayForEach(taskDef.selectors, (selector: Selector) => eventHandler.handle(mkEvent(testName(selector))))
    Array.empty

  private def mkEvent(testName: String): Event = new Event:
    override def selector: Selector = TestSelector(testName)
    override def status: Status = Status.Skipped
    override def duration: Long = 0
    override def throwable: OptionalThrowable = OptionalThrowable()
    override def fullyQualifiedName: String = taskDef.fullyQualifiedName
    override def fingerprint: Fingerprint = taskDef.fingerprint

  private def testName(selector: Selector): String = selector match
    case _                   : SuiteSelector        => "dry run"
    case _                   : NestedSuiteSelector  => "dry run"
    case testSelector        : TestSelector         => testSelector      .testName
    case nestedTestSelector  : NestedTestSelector   => nestedTestSelector.testName
    case testWildcardSelector: TestWildcardSelector => s"*${testWildcardSelector.testWildcard}*"
