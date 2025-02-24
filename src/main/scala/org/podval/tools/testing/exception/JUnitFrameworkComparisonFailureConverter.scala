package org.podval.tools.testing.exception

import junit.framework.ComparisonFailure
import org.gradle.api.tasks.testing.TestFailure

object JUnitFrameworkComparisonFailureConverter extends ExceptionConverter:
  override def toTestFailure(throwable: Throwable): TestFailure =
    val exception: ComparisonFailure = throwable.asInstanceOf[ComparisonFailure]
    TestFailure.fromTestAssertionFailure(exception, exception.getExpected, exception.getActual)
