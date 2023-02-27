package org.podval.tools.testing.exceptions

import org.gradle.api.tasks.testing.TestFailure

object JavaLangAssertionErrorConverter extends ExceptionConverter:
  override def convert(throwable: Throwable): TestFailure =
    val exception: AssertionError = throwable.asInstanceOf[AssertionError]
    TestFailure.fromTestAssertionFailure(exception, null, null)
