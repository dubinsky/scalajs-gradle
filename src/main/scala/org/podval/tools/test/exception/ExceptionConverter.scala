package org.podval.tools.test.exception

import org.gradle.api.tasks.testing.TestFailure

// Inspired by see org.gradle.api.internal.tasks.testing.junit.JUnitTestEventAdapter;
// see https://github.com/gradle/gradle/blob/master/platforms/jvm/testing-jvm-infrastructure/src/main/java/org/gradle/api/internal/tasks/testing/junit/JUnitTestEventAdapter.java
trait ExceptionConverter:
  def toTestFailure(throwable: Throwable): TestFailure

object ExceptionConverter:
  def exceptionConverter(throwableClassName: String): ExceptionConverter = throwableClassName match
    case "org.junit.ComparisonFailure" => OrgJUnitComparisonFailureConverter // JUnit
    case "junit.framework.ComparisonFailure" => JUnitFrameworkComparisonFailureConverter // JUnit
    case "munit.ComparisonFailException" => MUnitComparisonFailExceptionConverter // MUnit
    
    // Known exceptions that do not carry expected/actual data
    // (there are no framework-specific exceptions for ScalaCheck, specs2 nor ZIO Test).
    case
      "org.junit.internal.AssumptionViolatedException" | // JUni4
      "org.scalatest.exceptions.TestFailedException" | // ScalaTest
      "utest.AssertionError" | // UTest
      "java.lang.AssertionError" |
      "java.lang.Exception" |
      "org.scalajs.testing.common.Serializer$ThrowableSerializer$$anon$3" => // Scala.js wraps everything in
      DefaultExceptionConverter
      
    // Everything else
    case _ =>
      DefaultExceptionConverter
