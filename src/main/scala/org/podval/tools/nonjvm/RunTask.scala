package org.podval.tools.nonjvm

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.podval.tools.build.{BackendTask, TestEnvironment}
import org.podval.tools.gradle.{TaskWithDependency, TaskWithOutput, TaskWithRunner}
import org.podval.tools.test.task.TestTask
import scala.reflect.ClassTag

trait RunTask[B <: NonJvmBackend, L <: LinkTask[B] : ClassTag] extends BackendTask[B]
  with TaskWithDependency[L]
  with TaskWithOutput:
  
  final protected def linkTask: L = dependency

  protected def run: Run[B]

object RunTask:
  abstract class Main[B <: NonJvmBackend, L <: LinkTask.Main[B] : ClassTag] extends DefaultTask with RunTask[B, L] with TaskWithRunner:
    @TaskAction final def execute(): Unit = run.run(runner)

  abstract class Test[B <: NonJvmBackend, L <: LinkTask.Test[B] : ClassTag] extends TestTask[B] with RunTask[B, L]:
    protected def testEnvironmentCreator: TestEnvironment.Creator[B] = run
