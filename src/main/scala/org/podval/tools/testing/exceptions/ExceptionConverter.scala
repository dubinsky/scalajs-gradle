package org.podval.tools.testing.exceptions

import org.gradle.api.tasks.testing.TestFailure

// Note: converters have to be in separate objects because not all the frameworks
// (and thus exception classes) are guaranteed to be present on the classpath;
// exception class names can not be in those objects for the same reason.
// see org.gradle.api.internal.tasks.testing.junit.JUnitTestEventAdapter
trait ExceptionConverter:
  def toTestFailure(throwable: Throwable): TestFailure

object ExceptionConverter:
  private def converter(throwable: Throwable): ExceptionConverter = throwable.getClass.getName match
    case "org.junit.ComparisonFailure"       => OrgJUnitComparisonFailureConverter
    case "junit.framework.ComparisonFailure" => JUnitFrameworkComparisonFailureConverter
    case "munit.ComparisonFailException"     => MUnitComparisonFailExceptionConverter
    case "java.lang.AssertionError"          => JavaLangAssertionErrorConverter
    // TODO do ZIO Test and others?
    case _                                   => DefaultConverter

  def toTestFailure(throwable: Throwable): TestFailure =
    converter(throwable).toTestFailure(throwable)
