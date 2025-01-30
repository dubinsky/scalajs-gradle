package org.podval.tools.build

import org.gradle.api.{Project, Task}
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.tasks.{SourceSet, TaskProvider}
import org.gradle.api.tasks.scala.ScalaCompile
import scala.jdk.CollectionConverters.SetHasAsScala

object Gradle:
  def getConfiguration(project: Project, name: String): Configuration = project
    .getConfigurations
    .getByName(name)
  
  def getSourceSet(project: Project, name: String): SourceSet = project
    .getExtensions
    .getByType(classOf[JavaPluginExtension])
    .getSourceSets
    .getByName(name)
  
  def getClassesTask(project: Project, sourceSetName: String): Task = project
    .getTasks
    .getByName(getSourceSet(project, sourceSetName).getClassesTaskName)
  
  def getScalaCompile(project: Project, sourceSetName: String): ScalaCompile = getClassesTask(project, sourceSetName)
    .getDependsOn
    .asScala
    .find(classOf[TaskProvider[ScalaCompile]].isInstance)
    .get
    .asInstanceOf[TaskProvider[ScalaCompile]]
    .get

  def toOption[T](property: Property[T]): Option[T] =
    if !property.isPresent then None else Some(property.get)
    