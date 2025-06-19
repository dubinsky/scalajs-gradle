package org.podval.tools.nonjvm

import org.gradle.api.internal.provider.ProviderInternal
import org.gradle.api.{GradleException, Task}
import org.gradle.api.tasks.TaskProvider
import org.podval.tools.build.{BackendTask, LinkTask}
import scala.jdk.CollectionConverters.SetHasAsScala
import scala.reflect.ClassTag

trait TaskWithLink[L <: LinkTask : ClassTag] extends BackendTask:
  final protected def linkTask: L =
    val linkTaskClass: Class[L] = summon[ClassTag[L]].runtimeClass.asInstanceOf[Class[L]]
    findDependsOnProviderOrTask(linkTaskClass)
      .getOrElse(throw GradleException(s"Task $getName must depend on a task of type ${linkTaskClass.getName}!"))

  private def findDependsOnProviderOrTask[T <: Task](clazz: Class[? <: T]): Option[T] =
    findDependsOnTaskProvider(clazz)
      .map(_.get)
      .orElse(findDependsOnTask(clazz))

  private def findDependsOnTaskProvider[T <: Task](clazz: Class[? <: T]): Option[TaskProvider[T]] = this
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
