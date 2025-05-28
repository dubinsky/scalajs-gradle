package org.podval.tools.scalaplugin.nonjvm

import org.gradle.api.internal.provider.ProviderInternal
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.{GradleException, Task}
import org.podval.tools.scalaplugin.BackendTask
import scala.jdk.CollectionConverters.SetHasAsScala

trait NonJvmRunTask[L <: NonJvmLinkTask[L]] extends NonJvmTask[L]:
  protected def linkTaskClass: Class[? <: L]

  final override protected def linkTask: L = NonJvmRunTask
    .findDependsOnProviderOrTask(this, linkTaskClass)
    .getOrElse(throw GradleException(s"Task $getName must depend on a task of type ${linkTaskClass.getName}!"))

object NonJvmRunTask:
  abstract class Main[L <: NonJvmLinkTask[L]] extends BackendTask.Run.Main with NonJvmRunTask[L]
  abstract class Test[L <: NonJvmLinkTask[L]] extends BackendTask.Run.Test with NonJvmRunTask[L]

  private def findDependsOnProviderOrTask[T <: Task](task: Task, clazz: Class[? <: T]): Option[T] =
    findDependsOnTaskProvider(task, clazz)
      .map(_.get)
      .orElse(findDependsOnTask(task, clazz))

  private def findDependsOnTaskProvider[T <: Task](task: Task, clazz: Class[? <: T]): Option[TaskProvider[T]] = task
    .getDependsOn
    .asScala
    .filter(_.isInstanceOf[TaskProvider    [?]])
    .filter(_.isInstanceOf[ProviderInternal[?]])
    .map   (_.asInstanceOf[ProviderInternal[T]])
    .find(candidate => clazz.isAssignableFrom(candidate.getType))
    .map(_.asInstanceOf[TaskProvider[T]])

  private def findDependsOnTask[T <: Task](task: Task, clazz: Class[? <: T]): Option[T] = task
    .getDependsOn
    .asScala
    .filter(_.isInstanceOf[Task])
    .map   (_.asInstanceOf[Task])
    .find(candidate => clazz.isAssignableFrom(candidate.getClass))
    .map(_.asInstanceOf[T])
