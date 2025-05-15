package org.podval.tools.build

import org.gradle.api.{Project, Task}
import org.gradle.api.internal.provider.ProviderInternal
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.tasks.{SourceSet, TaskProvider}
import scala.jdk.CollectionConverters.SetHasAsScala

object Gradle:
  def getSourceSet(project: Project, name: String): SourceSet = project // TODO dissolve
    .getExtensions
    .getByType(classOf[JavaPluginExtension])
    .getSourceSets
    .getByName(name)

  def getClassesTaskProvider(project: Project, sourceSet: SourceSet): TaskProvider[Task] = project
    .getTasks
    .named(sourceSet.getClassesTaskName)
  
  def toOption[T](property: Property[T]): Option[T] =
    if !property.isPresent then None else Some(property.get)

  def findDependsOnProviderOrTask[T <: Task](task: Task, clazz: Class[? <: T]): Option[T] =
    findDependsOnTaskProvider(task, clazz)
      .map(_.get)
      .orElse(findDependsOnTask(task, clazz))

  def findDependsOnTaskProvider[T <: Task](task: Task, clazz: Class[? <: T]): Option[TaskProvider[T]] = task
    .getDependsOn
    .asScala
    .filter(_.isInstanceOf[TaskProvider[?]])
    .filter(_.isInstanceOf[ProviderInternal[?]])
    .map   (_.asInstanceOf[ProviderInternal[T]])
    .find  (candidate => clazz.isAssignableFrom(candidate.getType))
    .map   (_.asInstanceOf[TaskProvider[T]])

  def findDependsOnTask[T <: Task](task: Task, clazz: Class[? <: T]): Option[T] = task
    .getDependsOn
    .asScala
    .filter(_.isInstanceOf[Task])
    .map   (_.asInstanceOf[Task])
    .find  (candidate => clazz.isAssignableFrom(candidate.getClass))
    .map   (_.asInstanceOf[T])
