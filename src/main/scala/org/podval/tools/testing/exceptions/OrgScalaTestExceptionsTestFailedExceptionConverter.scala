package org.podval.tools.testing.exceptions

import org.scalatest.exceptions.TestFailedException
import org.gradle.api.tasks.testing.TestFailure

private object OrgScalaTestExceptionsTestFailedExceptionConverter extends ExceptionConverter:
  override def toTestFailure(throwable: Throwable): TestFailure =
    // TODO
    val exception: TestFailedException = throwable.asInstanceOf[TestFailedException]
    TestFailure.fromTestAssertionFailure(exception, null, null)
