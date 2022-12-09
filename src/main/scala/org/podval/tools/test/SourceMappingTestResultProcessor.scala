package org.podval.tools.test

import org.gradle.api.internal.tasks.testing.{TestCompleteEvent, TestDescriptorInternal, TestResultProcessor, TestStartEvent}
import org.gradle.api.tasks.testing.{TestFailure, TestOutputEvent}

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

  override def failure(testId: Object, testFailure: TestFailure): Unit =
    delegate.failure(testId, sourceMapper.fold(testFailure)(_.sourceMap(testFailure)))
