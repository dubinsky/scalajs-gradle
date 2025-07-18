package org.podval.tools.gradle

import org.gradle.api.{GradleException, Task}
import org.gradle.api.internal.provider.ProviderInternal
import org.gradle.api.tasks.TaskProvider
import scala.reflect.ClassTag
import scala.jdk.CollectionConverters.SetHasAsScala

trait TaskWithDependency[D <: Task : ClassTag] extends Task:
  private def dependencyTaskClass: Class[D] = summon[ClassTag[D]].runtimeClass.asInstanceOf[Class[D]]
  
  private def is[T](clazz: T => Class[?])(candidate: T): Boolean =
    dependencyTaskClass.isAssignableFrom(clazz(candidate))

  final protected def dependency: D = findDependsOnTaskProvider
    .map(_.get)
    .orElse(findDependsOnTask)
    .getOrElse(throw GradleException(s"Task $getName must depend on a task of type ${dependencyTaskClass.getName}!"))

  private def findDependsOnTaskProvider: Option[TaskProvider[D]] = getDependsOn
    .asScala
    .filter(_.isInstanceOf[TaskProvider[?]])
    .filter(_.isInstanceOf[ProviderInternal[?]])
    .map(_.asInstanceOf[ProviderInternal[D]])
    .find(is(_.getType))
    .map(_.asInstanceOf[TaskProvider[D]])

  private def findDependsOnTask: Option[D] = getDependsOn
    .asScala
    .filter(_.isInstanceOf[Task])
    .map(_.asInstanceOf[Task])
    .find(is(_.getClass))
    .map(_.asInstanceOf[D])
