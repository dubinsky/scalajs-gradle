package org.podval.tools.nonjvm

import org.gradle.api.{DefaultTask, GradleException, Task}
import org.gradle.api.internal.provider.ProviderInternal
import org.gradle.api.tasks.{CacheableTask, TaskAction, TaskProvider}
import org.podval.tools.build.{Backend, OutputTask, RunnerTask, TestEnvironment, TestTask}
import scala.reflect.ClassTag
import scala.jdk.CollectionConverters.SetHasAsScala

trait RunTask[B <: NonJvmBackend, L <: LinkTask[B] : ClassTag] extends Backend.Task[B]
  with OutputTask:

  protected def run: Run[B]

  final protected def linkTask: L = findDependsOnTaskProvider
    .map(_.get)
    .orElse(findDependsOnTask)
    .getOrElse(throw GradleException(s"Task $getName must depend on a task of type ${dependencyTaskClass.getName}!"))

  private def findDependsOnTaskProvider: Option[TaskProvider[L]] = getDependsOn
    .asScala
    .filter(_.isInstanceOf[TaskProvider[?]])
    .filter(_.isInstanceOf[ProviderInternal[?]])
    .map(_.asInstanceOf[ProviderInternal[L]])
    .find(is(_.getType))
    .map(_.asInstanceOf[TaskProvider[L]])

  private def findDependsOnTask: Option[L] = getDependsOn
    .asScala
    .filter(_.isInstanceOf[Task])
    .map(_.asInstanceOf[Task])
    .find(is(_.getClass))
    .map(_.asInstanceOf[L])

  private def dependencyTaskClass: Class[L] = summon[ClassTag[L]].runtimeClass.asInstanceOf[Class[L]]

  private def is[T](clazz: T => Class[?])(candidate: T): Boolean =
    dependencyTaskClass.isAssignableFrom(clazz(candidate))

object RunTask:
  @CacheableTask
  abstract class Main[B <: NonJvmBackend, L <: LinkTask.Main[B] : ClassTag] extends DefaultTask with RunTask[B, L] with RunnerTask:
    @TaskAction final def execute(): Unit = run.run(runner)

  @CacheableTask
  abstract class Test[B <: NonJvmBackend, L <: LinkTask.Test[B] : ClassTag] extends TestTask[B] with RunTask[B, L]:
    final override protected def testEnvironmentCreator: TestEnvironment.Creator[B] = run
