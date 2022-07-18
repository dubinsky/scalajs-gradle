package org.podval.tools.test

import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.gradle.api.logging.LogLevel
import TestResultProcessorEx.*

final class TestLogger(
  testResultProcessor: TestResultProcessor,
  test: TestClass,
  useColours: Boolean
) extends sbt.testing.Logger:
  private def log(logLevel: LogLevel, message: String): Unit = testResultProcessor.log(
    testId = test.getId,
    message = message,
    logLevel = logLevel
  )
  override def error(message: String): Unit = log(LogLevel.ERROR, message)
  override def warn (message: String): Unit = log(LogLevel.WARN , message)
  override def info (message: String): Unit = log(LogLevel.INFO , message)
  override def debug(message: String): Unit = log(LogLevel.DEBUG, message)
  override def trace(t: Throwable): Unit = testResultProcessor.failure(test.getId, t)
  override def ansiCodesSupported: Boolean = useColours
