package org.podval.tools.testing.exceptions

import munit.ComparisonFailException
import org.gradle.api.tasks.testing.TestFailure

object MUnitComparisonFailExceptionConverter extends ExceptionConverter:
  override def toTestFailure(throwable: Throwable): TestFailure =
    val exception: ComparisonFailException = throwable.asInstanceOf[ComparisonFailException]
    TestFailure.fromTestAssertionFailure(exception, exception.expected.toString, exception.obtained.toString)
