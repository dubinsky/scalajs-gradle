package org.podval.tools.scalajs.testing

// Note: based on sbt.TestResult from org.scala-sbt.testing
sealed abstract class TestResult(val severity: Int) derives CanEqual

object TestResult:
  case object Passed extends TestResult(0)
  case object Failed extends TestResult(1)
  case object Error  extends TestResult(2)
