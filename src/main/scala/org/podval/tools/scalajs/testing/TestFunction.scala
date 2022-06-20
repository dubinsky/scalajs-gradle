package org.podval.tools.scalajs.testing

import sbt.testing.{Task as TestTask, TaskDef}

// Note: based on sbt.TestFramework from org.scala-sbt.testing
final class TestFunction(
  val taskDef: TaskDef,
  val runner: TestRunner,
  testTask: TestTask
):
  def run(): (SuiteResult, Seq[TestTask]) = runner.run(taskDef, testTask)

  def tags: Seq[String] = testTask.tags.toIndexedSeq
