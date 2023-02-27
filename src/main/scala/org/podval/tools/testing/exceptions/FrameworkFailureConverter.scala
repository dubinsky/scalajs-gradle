package org.podval.tools.testing.exceptions

import org.gradle.api.tasks.testing.TestFailure

object FrameworkFailureConverter extends ExceptionConverter:
  override def convert(throwable: Throwable): TestFailure =
    TestFailure.fromTestFrameworkFailure(throwable)
