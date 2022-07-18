package org.podval.tools.test

import org.gradle.api.internal.tasks.testing.{TestCompleteEvent, TestDescriptorInternal, TestResultProcessor, TestStartEvent}
import org.gradle.api.tasks.testing.TestOutputEvent

final class SourceMappingTestResultProcessor(
  delegate: TestResultProcessor,
  sourceMapper: Option[SourceMapper]
) extends TestResultProcessor:

  override def started(test: TestDescriptorInternal, event: TestStartEvent): Unit =
    delegate.started(test, event)

  override def completed(testId: Object, event: TestCompleteEvent): Unit =
    delegate.completed(testId, event)

  override def output(testId: Object, event: TestOutputEvent): Unit =
    delegate.output(testId, event)

  //  def failure(testId: Any, result: TestFailure): Unit
  override def failure(testId: Object, throwable: Throwable): Unit =
    delegate.failure(testId, sourceMapper.fold(throwable)(_.sourceMap(throwable)))
