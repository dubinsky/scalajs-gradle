package org.podval.tools.test.run

import org.gradle.api.internal.tasks.testing.{DefaultTestClassDescriptor, DefaultTestMethodDescriptor,
  DefaultTestOutputEvent, TestCompleteEvent, TestResultProcessor, TestStartEvent}
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.testing.TestOutputEvent
import org.gradle.api.tasks.testing.TestResult.ResultType
import org.gradle.internal.id.IdGenerator
import org.gradle.internal.time.Clock
import org.podval.tools.test.exception.ExceptionConverter

final class TestResultProcessorEx(
  testResultProcessor: TestResultProcessor,
  logLevelEnabled: LogLevel,
  clock: Clock,
  idGenerator: IdGenerator[?]
):
  def getCurrentTime: Long = clock.getCurrentTime

  def generateId(): AnyRef = idGenerator.generateId()

  def started(
    parentId: AnyRef,
    testId: AnyRef,
    testClassName: String,
    testName: Option[String],
    startTime: Long
  ): Unit = testResultProcessor.started(
    testName match
      case None           => DefaultTestClassDescriptor (testId, testClassName)
      case Some(testName) => DefaultTestMethodDescriptor(testId, testClassName, testName),
    TestStartEvent(startTime, parentId)
  )

  def completed(
    testId: AnyRef,
    endTime: Long,
    result: ResultType
  ): Unit = testResultProcessor.completed(
    testId,
    TestCompleteEvent(endTime, result)
  )

  def failure(
    testId: AnyRef,
    throwable: Throwable
  ): Unit = testResultProcessor.failure(
    testId,
    ExceptionConverter.exceptionConverter(throwable.getClass.getName).toTestFailure(throwable)
  )

  def output(
    logLevel: LogLevel,
    testId: AnyRef,
    message: String,
  ): Unit =
    given CanEqual[LogLevel, LogLevel] = CanEqual.derived
    if logLevel.ordinal >= logLevelEnabled.ordinal then testResultProcessor.output(
      testId,
      DefaultTestOutputEvent(
        clock.getCurrentTime,
        if (logLevel == LogLevel.ERROR) || (logLevel == LogLevel.WARN)
        then TestOutputEvent.Destination.StdErr
        else TestOutputEvent.Destination.StdOut,
        s"$message\n"
      )
    )
