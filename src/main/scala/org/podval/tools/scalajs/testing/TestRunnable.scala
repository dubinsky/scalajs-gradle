package org.podval.tools.scalajs.testing

import org.opentorah.util.Collections
import sbt.testing.{TaskDef, Task as TestTask}

// Note: based on sbt.TestFramework from org.scala-sbt.testing
final class TestRunnable(
  name: String,
  val taskDef: TaskDef,
  testTask: TestTask,
  val runner: TestRunner
):

  def tags: Seq[String] = testTask.tags.toIndexedSeq

  private def toTask: Task[Map[String, SuiteResult]] =
    for
      (name: String, (result: SuiteResult, nested: Seq[TestTask])) <- Task((name, runner.run(taskDef, testTask)))
      nestedRunnables: Seq[TestRunnable] = createNestedRunnables(nested)
      currentResultMap: Map[String, SuiteResult] <- TestRunnable.toTask(nestedRunnables)
    yield
      currentResultMap.updated(name, currentResultMap.get(name) match
        case Some(currentResult) => currentResult.add(result)
        case None                => result
      )

  private def createNestedRunnables(nestedTasks: Seq[TestTask]): Seq[TestRunnable] =
    for (nestedTask: TestTask, idx: Int) <- nestedTasks.zipWithIndex yield TestRunnable(
      name = taskDef.fullyQualifiedName,
      taskDef = TaskDef(
        taskDef.fullyQualifiedName + "-" + idx,
        taskDef.fingerprint,
        taskDef.explicitlySpecified,
        taskDef.selectors
      ),
      testTask = nestedTask,
      runner = runner
    )

object TestRunnable:
  def toTask(runnables: Seq[TestRunnable]): Task[Map[String, SuiteResult]] =
    for suiteResults: Seq[Map[String, SuiteResult]] <- Task.join(runnables.map(_.toTask)) yield
      suiteResults.foldLeft(Map.empty[String, SuiteResult]) {
        case (sum: Map[String, SuiteResult], e: Map[String, SuiteResult]) =>
          val grouped: Map[String, Seq[(String, SuiteResult)]] = (sum.toSeq ++ e.toSeq).groupBy(_._1)
          Collections.mapValues(grouped)(_.map(_._2).foldLeft(SuiteResult.Empty) {
            case (acc: SuiteResult, result: SuiteResult) => acc.add(result)
          })
      }
