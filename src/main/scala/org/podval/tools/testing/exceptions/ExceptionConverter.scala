package org.podval.tools.testing.exceptions

import org.gradle.api.tasks.testing.TestFailure

// Note: converters have to be in separate objects because not all the frameworks
// (and thus exception classes) are guaranteed to be present on the classpath;
// exception class names can not be in those objects for the same reason.
// see org.gradle.api.internal.tasks.testing.junit.JUnitTestEventAdapter
trait ExceptionConverter:
  def toTestFailure(throwable: Throwable): TestFailure
