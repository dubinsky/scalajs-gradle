package org.podval.tools.testing.exceptions

import junit.framework.ComparisonFailure
import org.gradle.api.tasks.testing.TestFailure

object JUnitFrameworkComparisonFailureConverter extends ExceptionConverter:
  override def convert(throwable: Throwable): TestFailure =
    val exception: ComparisonFailure = throwable.asInstanceOf[ComparisonFailure]
    TestFailure.fromTestAssertionFailure(exception, exception.getExpected, exception.getActual)
