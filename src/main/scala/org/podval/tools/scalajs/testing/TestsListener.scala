package org.podval.tools.scalajs.testing

import org.gradle.api.tasks.testing.TestResult.ResultType

// Note: based on sbt.TestFramework from org.scala-sbt.testing
trait TestsListener:

  /** called once, at beginning. */
  def doInit(): Unit

  /** called once, at end of the test group. */
  def doComplete(finalResult: ResultType): Unit

  /** called for each class or equivalent grouping */
  def startGroup(name: String): Unit

  /** called for each test method or equivalent */
  def testEvent(event: TestEvent): Unit

  /** called if there was an error during test */
  def endGroup(name: String, t: Throwable): Unit

  /** called if test completed */
  def endGroup(name: String, result: ResultType): Unit

  /** Used by the test framework for logging test results */
  def contentLogger(@deprecated("unused", "") name: String): Option[ContentLogger] = None
