package org.podval.tools.testing.exception

import org.junit.internal.AssumptionViolatedException
import org.gradle.api.tasks.testing.TestFailure

object OrgJUnitInternalAssumptionViolatedExceptionConverter extends ExceptionConverter:
    override def toTestFailure(throwable: Throwable): TestFailure =
      // TODO
      val exception: AssumptionViolatedException = throwable.asInstanceOf[AssumptionViolatedException]
      TestFailure.fromTestAssertionFailure(exception, null, null)
