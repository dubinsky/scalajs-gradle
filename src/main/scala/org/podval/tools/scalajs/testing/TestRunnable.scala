package org.podval.tools.scalajs.testing

import org.opentorah.util.Collections
import sbt.testing.{TaskDef, Task as TestTask}

final class TestRunnable(
  name: String,
  testFunction: TestFunction
):
  def toTask: Task[Map[String, SuiteResult]] =
    Task[(String, (SuiteResult, Seq[TestTask]))]((name, testFunction.run()))
      .flatMap { case (name: String, (result: SuiteResult, nested: Seq[TestTask])) =>
        TestRunnable.toTasks(createNestedRunnables(nested)).map { (currentResultMap: Map[String, SuiteResult]) =>
          val newResult: SuiteResult = currentResultMap.get(name) match
            case Some(currentResult) => currentResult + result
            case None                => result
          currentResultMap.updated(name, newResult)
        }
      }

  private def createNestedRunnables(nestedTasks: Seq[TestTask]): Seq[TestRunnable] =
    for (nt: TestTask, idx: Int) <- nestedTasks.zipWithIndex yield
      val testFunDef: TaskDef = testFunction.taskDef
      TestRunnable(
        testFunDef.fullyQualifiedName,
        TestFunction(
          TaskDef(
            testFunDef.fullyQualifiedName + "-" + idx,
            testFunDef.fingerprint,
            testFunDef.explicitlySpecified,
            testFunDef.selectors
          ),
          testFunction.runner,
          nt
        )
      )

object TestRunnable:
  def toTasks(runnables: Seq[TestRunnable]): Task[Map[String, SuiteResult]] =
    Task.join[Map[String, SuiteResult]](runnables.map(_.toTask))
      .map(_.foldLeft(Map.empty[String, SuiteResult]) {
        case (sum: Map[String, SuiteResult], e: Map[String, SuiteResult]) =>
          val grouped: Map[String, Seq[(String, SuiteResult)]] = (sum.toSeq ++ e.toSeq).groupBy(_._1)
          Collections.mapValues(grouped)(_.map(_._2).foldLeft(SuiteResult.Empty) {
            case (acc: SuiteResult, result: SuiteResult) => acc + result
          })
      })
