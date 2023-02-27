package org.podval.tools.testing.exceptions

import org.gradle.api.tasks.testing.TestFailure
import org.junit.ComparisonFailure

object OrgJUnitComparisonFailureConverter extends ExceptionConverter:
  override def convert(throwable: Throwable): TestFailure =
    val exception: ComparisonFailure = throwable.asInstanceOf[ComparisonFailure]
    TestFailure.fromTestAssertionFailure(exception, exception.getExpected, exception.getActual)
