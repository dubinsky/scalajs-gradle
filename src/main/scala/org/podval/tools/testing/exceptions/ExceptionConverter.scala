package org.podval.tools.testing.exceptions

import org.gradle.api.tasks.testing.TestFailure

// Note: converters have to be in separate objects because not all the frameworks
// (and thus exception classes) are guaranteed to be present on the classpath;
// exception class names can not be in those objects for the same reason.
// see org.gradle.api.internal.tasks.testing.junit.JUnitTestEventAdapter
trait ExceptionConverter:
  def toTestFailure(throwable: Throwable): TestFailure

object ExceptionConverter:
  def converter(throwableClassName: String): ExceptionConverter = throwableClassName match
    case "org.junit.ComparisonFailure"                  => OrgJUnitComparisonFailureConverter
    case "junit.framework.ComparisonFailure"            => JUnitFrameworkComparisonFailureConverter
    case "munit.ComparisonFailException"                => MUnitComparisonFailExceptionConverter
    case "java.lang.AssertionError"                     => JavaLangAssertionErrorConverter
    case "org.scalatest.exceptions.TestFailedException" => OrgScalaTestExceptionsTestFailedExceptionConverter
    case "utest.AssertionError"                         => UTestAssertionErrorConverter
    // Note: there is no ScalaCheck-specific exception
    // Note: there is no specs2-specific exception
    // Note: there is no ZIO Test-specific exception
    case _                                              => DefaultConverter
