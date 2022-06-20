package org.podval.tools.scalajs.testing

import org.gradle.api.logging.Logger
import sbt.testing.{Framework, Runner, TaskDef, Task as TestTask}

final class FrameworkRun(
  framework: Framework,
  tests: Set[TestDefinition],
  listeners: Seq[TestsListener],
  log: Logger
):
  // TODO are there any ScalaTest parameters that control logging of the test outputs and such?
  private val runner: Runner = framework.runner(Array.empty[String], Array.empty[String], null: ClassLoader)

  private val testRunner: TestRunner = TestRunner(runner, listeners, log)

  def testTasks: Seq[TestRunnable] =
    for testTask: TestTask <- testRunner.tasks(tests).toSeq yield
      val taskDef: TaskDef = testTask.taskDef
      TestRunnable(taskDef.fullyQualifiedName, TestFunction(taskDef, testRunner, testTask))
