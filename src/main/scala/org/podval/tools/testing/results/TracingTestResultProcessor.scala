package org.podval.tools.testing.results

import org.gradle.api.internal.tasks.testing.{TestCompleteEvent, TestDescriptorInternal, TestResultProcessor, TestStartEvent}
import org.gradle.api.tasks.testing.{TestFailure, TestOutputEvent}
import org.gradle.internal.time.Clock

class TracingTestResultProcessor(
  delegate: TestResultProcessor,
  clock: Clock,
  isEnabled: Boolean
) extends TestResultProcessor:
  private def trace(message: String): Unit =
    if isEnabled then println(clock.getCurrentTime.toString + "  " + message)

  override def started(test: TestDescriptorInternal, event: TestStartEvent): Unit =
    trace(s"started: id=${test.getId} $test parentId=${event.getParentId} class=${test.getClass.getName} isComposite=${test.isComposite}")
    delegate.started(test, event)

  override def completed(testId: AnyRef, event: TestCompleteEvent): Unit =
    trace(s"completed: id=$testId resultType=${event.getResultType}")
    delegate.completed(testId, event)

  override def output(testId: AnyRef, event: TestOutputEvent): Unit =
    trace(s"output: id=$testId message=${event.getMessage}")
    delegate.output(testId, event)

  override def failure(testId: AnyRef, failure: TestFailure): Unit =
    trace(s"failure: id=$testId ${failure.getRawFailure.getCause}")
    delegate.failure(testId, failure)
