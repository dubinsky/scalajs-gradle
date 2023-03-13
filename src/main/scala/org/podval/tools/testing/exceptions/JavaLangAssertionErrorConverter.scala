package org.podval.tools.testing.exceptions

import org.gradle.api.tasks.testing.TestFailure

private object JavaLangAssertionErrorConverter extends ExceptionConverter:
  override def toTestFailure(throwable: Throwable): TestFailure =
    val exception: AssertionError = throwable.asInstanceOf[AssertionError]
    TestFailure.fromTestAssertionFailure(exception, null, null)

