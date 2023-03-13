package org.podval.tools.testing.exceptions

import org.gradle.api.tasks.testing.{TestExecutionException, TestFailure}

private object DefaultConverter extends ExceptionConverter:
  override def toTestFailure(throwable: Throwable): TestFailure = TestFailure.fromTestFrameworkFailure(throwable)
