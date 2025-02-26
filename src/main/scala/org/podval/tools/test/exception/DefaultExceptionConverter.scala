package org.podval.tools.test.exception

import org.gradle.api.tasks.testing.TestFailure

object DefaultExceptionConverter extends ExceptionConverter:
  override def toTestFailure(throwable: Throwable): TestFailure =
    TestFailure.fromTestFrameworkFailure(throwable)
