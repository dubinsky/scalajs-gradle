package org.podval.tools.testing.exception

import utest.AssertionError
import org.gradle.api.tasks.testing.TestFailure

object UTestAssertionErrorConverter extends ExceptionConverter:
  override def toTestFailure(throwable: Throwable): TestFailure =
    // TODO
    val exception: AssertionError = throwable.asInstanceOf[AssertionError]
    TestFailure.fromTestAssertionFailure(exception, null, null)
