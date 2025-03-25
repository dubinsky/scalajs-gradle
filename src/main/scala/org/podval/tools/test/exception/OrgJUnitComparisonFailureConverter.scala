package org.podval.tools.test.exception

import org.junit.ComparisonFailure
import org.gradle.api.tasks.testing.TestFailure

object OrgJUnitComparisonFailureConverter extends ExceptionConverter:
  override def toTestFailure(throwable: Throwable): TestFailure =
    val exception: ComparisonFailure = throwable.asInstanceOf[ComparisonFailure]
    TestFailure.fromTestAssertionFailure(exception, exception.getExpected, exception.getActual)
