package org.podval.tools.test

import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.gradle.api.logging.LogLevel
import TestResultProcessorEx.*

final class TestLogger(
  testResultProcessor: TestResultProcessor,
  test: Test,
  useColours: Boolean
) extends sbt.testing.Logger:
  private def log(logLevel: LogLevel, message: String): Unit = testResultProcessor.log(
    test = test,
    message = message,
    logLevel = logLevel
  )
  override def error(message: String): Unit = log(LogLevel.ERROR, message)
  override def warn (message: String): Unit = log(LogLevel.WARN , message)
  override def info (message: String): Unit = log(LogLevel.INFO , message)
  override def debug(message: String): Unit = log(LogLevel.DEBUG, message)
  override def trace(throwable: Throwable): Unit = testResultProcessor.failed(test = test, throwable = throwable)
  override def ansiCodesSupported: Boolean = useColours
