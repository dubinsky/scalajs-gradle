package org.podval.tools.nonjvm

import org.gradle.api.{DefaultTask, GradleException, Task}
import org.gradle.api.internal.provider.ProviderInternal
import org.gradle.api.tasks.{TaskAction, TaskProvider}
import org.podval.tools.build.{BackendTask, TestEnvironment}
import org.podval.tools.platform.TaskWithRunner
import org.podval.tools.test.task.TestTask
import scala.reflect.ClassTag
import scala.jdk.CollectionConverters.SetHasAsScala

trait RunTask[B <: NonJvmBackend, L <: LinkTask[B] : ClassTag] extends BackendTask[B]:
  protected def run: Run[B]

  final protected def linkTask: L =
    val linkTaskClass: Class[L] = summon[ClassTag[L]].runtimeClass.asInstanceOf[Class[L]]
    findDependsOnProviderOrTask(linkTaskClass)
      .getOrElse(throw GradleException(s"Task $getName must depend on a task of type ${linkTaskClass.getName}!"))

  private def findDependsOnProviderOrTask[T <: Task](clazz: Class[? <: T]): Option[T] =
    findDependsOnTaskProvider(clazz)
      .map(_.get)
      .orElse(findDependsOnTask(clazz))

  private def findDependsOnTaskProvider[T <: Task](clazz: Class[T]): Option[TaskProvider[T]] = this
    .getDependsOn
    .asScala
    .filter(_.isInstanceOf[TaskProvider[?]])
    .filter(_.isInstanceOf[ProviderInternal[?]])
    .map(_.asInstanceOf[ProviderInternal[T]])
    .find(candidate => clazz.isAssignableFrom(candidate.getType))
    .map(_.asInstanceOf[TaskProvider[T]])

  private def findDependsOnTask[T <: Task](clazz: Class[? <: T]): Option[T] = this
    .getDependsOn
    .asScala
    .filter(_.isInstanceOf[Task])
    .map(_.asInstanceOf[Task])
    .find(candidate => clazz.isAssignableFrom(candidate.getClass))
    .map(_.asInstanceOf[T])

object RunTask:
  abstract class Main[B <: NonJvmBackend, L <: LinkTask.Main[B] : ClassTag] extends DefaultTask with RunTask[B, L] with TaskWithRunner:
    @TaskAction final def execute(): Unit = run.run(runner)

  abstract class Test[B <: NonJvmBackend, L <: LinkTask.Test[B] : ClassTag] extends TestTask[B] with RunTask[B, L]:
    protected def testEnvironmentCreator: TestEnvironment.Creator[B] = run
