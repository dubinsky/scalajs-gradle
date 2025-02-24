package org.podval.tools.testing.exception

import org.gradle.api.tasks.testing.TestFailure
import org.junit.ComparisonFailure

object OrgJUnitComparisonFailureConverter extends ExceptionConverter:
  override def toTestFailure(throwable: Throwable): TestFailure =
    val exception: ComparisonFailure = throwable.asInstanceOf[ComparisonFailure]
    TestFailure.fromTestAssertionFailure(exception, exception.getExpected, exception.getActual)
