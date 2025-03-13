package org.podval.tools.test.run

import org.gradle.api.internal.tasks.testing.{TestCompleteEvent, TestDescriptorInternal, TestResultProcessor, TestStartEvent}
import org.gradle.api.tasks.testing.{TestFailure, TestOutputEvent}
import org.slf4j.{Logger, LoggerFactory}

class TracingTestResultProcessor(delegate: TestResultProcessor) extends TestResultProcessor:
  private val logger: Logger = LoggerFactory.getLogger(classOf[TracingTestResultProcessor])
  
  private def trace(message: String): Unit = logger.info(s"TracingTestResultProcessor: $message")

  trace(s"TestResultProcessor class: ${delegate.getClass.getName}")

  override def output(testId: AnyRef, event: TestOutputEvent): Unit =
    // Not tracing the output: it already gets printed at level INFO by Gradle
    //trace(s"output: id=$testId message=${event.getMessage}")
    delegate.output(testId, event)

  override def started(test: TestDescriptorInternal, event: TestStartEvent): Unit =
    trace(s"started: id=${test.getId} $test parentId=${event.getParentId} class=${test.getClass.getName} isComposite=${test.isComposite}")
    delegate.started(test, event)

  override def completed(testId: AnyRef, event: TestCompleteEvent): Unit =
    trace(s"completed: id=$testId resultType=${event.getResultType}")
    delegate.completed(testId, event)
  
  override def failure(testId: AnyRef, failure: TestFailure): Unit =
    trace(s"failure: id=$testId ${failure.getRawFailure.getCause}")
    delegate.failure(testId, failure)
