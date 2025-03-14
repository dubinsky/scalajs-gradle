package org.podval.tools.test.result

import org.gradle.api.internal.tasks.testing.{TestCompleteEvent, TestDescriptorInternal, TestResultProcessor, 
  TestStartEvent}
import org.gradle.api.tasks.testing.{TestFailure, TestOutputEvent}
import org.gradle.internal.id.CompositeIdGenerator.CompositeId
import org.podval.tools.test.RootTestSuiteIdPlaceholder

class RootTestSuiteOutputFixingTestResultProcessor(
  delegate: TestResultProcessor
) extends TestResultProcessor:

  private var rootId: Option[AnyRef] = None

  override def started(test: TestDescriptorInternal, event: TestStartEvent): Unit =
    if rootId.isEmpty then rootId = Some(test.getId)
    delegate.started(test, event)

  override def completed(testId: AnyRef, event: TestCompleteEvent): Unit =
    delegate.completed(testId, event)

  override def output(testId: AnyRef, event: TestOutputEvent): Unit =
    given CanEqual[CompositeId, CompositeId] = CanEqual.derived
    val testIdEffective: AnyRef =
      if testId.asInstanceOf[CompositeId] != RootTestSuiteIdPlaceholder.value
      then testId
      else rootId.get
    delegate.output(testIdEffective, event)

  override def failure(testId: AnyRef, failure: TestFailure): Unit =
    delegate.failure(testId, failure)
