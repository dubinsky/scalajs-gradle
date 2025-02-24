package org.podval.tools.testing.exception

import org.gradle.api.tasks.testing.TestFailure

// Note: converters have to be in separate objects because not all the frameworks
// (and thus exception classes) are guaranteed to be present on the classpath;
// exception class names can not be in those objects for the same reason.
// see org.gradle.api.internal.tasks.testing.junit.JUnitTestEventAdapter
// https://github.com/gradle/gradle/blob/master/platforms/jvm/testing-jvm-infrastructure/src/main/java/org/gradle/api/internal/tasks/testing/junit/JUnitTestEventAdapter.java
trait ExceptionConverter:
  def toTestFailure(throwable: Throwable): TestFailure

object ExceptionConverter:
  def exceptionConverter(throwableClassName: String): ExceptionConverter = throwableClassName match
    case "org.junit.ComparisonFailure" => // JUnit
      OrgJUnitComparisonFailureConverter
    case "org.junit.internal.AssumptionViolatedException" =>
      OrgJUnitInternalAssumptionViolatedExceptionConverter
    case "junit.framework.ComparisonFailure" => // JUnit
      JUnitFrameworkComparisonFailureConverter
    case "munit.ComparisonFailException" => // MUnit
      MUnitComparisonFailExceptionConverter
    case "org.scalatest.exceptions.TestFailedException" => // ScalaTest
      OrgScalaTestExceptionsTestFailedExceptionConverter
    case "utest.AssertionError" => // UTest
      UTestAssertionErrorConverter
    case "java.lang.AssertionError" =>
      JavaLangAssertionErrorConverter
    case "java.lang.Exception" =>
      DefaultExceptionConverter
    case "org.scalajs.testing.common.Serializer$ThrowableSerializer$$anon$3" => // Scala.js
      DefaultExceptionConverter
    // Everything else (there is no framework-specific exceptions for ScalaCheck, specs2 nor ZIO Test):  
    case _ =>
//      throw IllegalArgumentException(s"--- Unknown Throwable class name: $throwableClassName")
      DefaultExceptionConverter
