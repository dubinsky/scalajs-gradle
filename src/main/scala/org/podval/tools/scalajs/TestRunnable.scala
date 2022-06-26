package org.podval.tools.scalajs

import org.gradle.api.tasks.testing.TestResult
import sbt.testing.{TaskDef, Task as TestTask}

// Note: based on sbt.TestFramework from org.scala-sbt.testing
final class TestRunnable(
  name: String,
  testDefinition: TestDefinition,
  testTask: TestTask,
  runner: TestRunner
):
  // TODO combine apply() and toTask() and make the class private
  // TODO move groupStart/end here
//  def tags: Seq[String] = testTask.tags.toIndexedSeq

  def toTask: Task[Map[String, TestResult]] = for
    // TODO what are nested tasks?
    (result: TestResult, nestedTestTasks: Seq[TestTask]) <- Task(runner.run(testDefinition, testTask))
    nestedTasks: Seq[Task[Map[String, TestResult]]] =
      for (nestedTestTask: TestTask, index: Int) <- nestedTestTasks.zipWithIndex yield
        val taskDef: TaskDef = testDefinition.taskDef
        TestRunnable(
          runner = runner,
          testTask = nestedTestTask,
          name = taskDef.fullyQualifiedName,
          testDefinition = TestDefinition(
            isComposite = false,
            taskDef = TaskDef(
              taskDef.fullyQualifiedName + "-" + index,
              taskDef.fingerprint,
              taskDef.explicitlySpecified,
              taskDef.selectors
            )
          )
        ).toTask
    currentResultMap: Map[String, TestResult] <- Tests.combineSuiteResults(nestedTasks)
  yield currentResultMap.updated(name, currentResultMap.get(name) match
    case Some(currentResult) => Tests.addTestResults(result, currentResult)
    case None                => result
  )
