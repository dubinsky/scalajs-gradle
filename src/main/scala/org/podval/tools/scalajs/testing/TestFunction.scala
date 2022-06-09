package org.podval.tools.scalajs.testing

import sbt.testing.{Task as TestTask, TaskDef}

// Note: based on sbt.TestFramework from org.scala-sbt.testing
abstract class TestFunction(
  val taskDef: TaskDef,
  val runner: TestRunner,
  fun: (TestRunner) => (SuiteResult, Seq[TestTask])
):
  def apply(): (SuiteResult, Seq[TestTask]) = fun(runner)

  def tags: Seq[String]
