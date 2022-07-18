package org.podval.tools.test

import org.gradle.api.internal.tasks.testing.{DefaultTestOutputEvent, TestCompleteEvent, TestResultProcessor, TestStartEvent}
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.testing.TestOutputEvent
import org.gradle.api.tasks.testing.TestResult.ResultType

object TestResultProcessorEx:
  private given CanEqual[LogLevel, LogLevel] = CanEqual.derived

  extension (testResultProcessor: TestResultProcessor)
    def started(
      test: Test,
      startTime: Long
    ): Unit = testResultProcessor.started(
      test,
      TestStartEvent(
        startTime,
        test.getParentId
      )
    )

    def completed(
      test: Test,
      endTime: Long,
      resultType: ResultType = ResultType.SUCCESS
    ): Unit = testResultProcessor.completed(
      test.getId,
      TestCompleteEvent(
        endTime,
        resultType
      )
    )

    def output(
      testId: Object,
      message: String,
      isError: Boolean
    ): Unit = testResultProcessor.output(
      testId,
      DefaultTestOutputEvent(
        if isError
        then TestOutputEvent.Destination.StdErr
        else TestOutputEvent.Destination.StdOut,
        message
      )
    )

    // TODO is there a way to convey the actual LogLevel, not just isError?
    def log(
      testId: Object,
      message: String,
      logLevel: LogLevel
    ): Unit = testResultProcessor.output(
      testId = testId,
      message = message,
      isError = (logLevel == LogLevel.ERROR) || (logLevel == LogLevel.WARN)
    )
