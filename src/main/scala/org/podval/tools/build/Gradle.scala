package org.podval.tools.build

import org.gradle.api.{Project, Task}
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.tasks.{SourceSet, SourceSetContainer, TaskProvider}
import org.gradle.api.tasks.scala.ScalaCompile
import scala.jdk.CollectionConverters.SetHasAsScala

// TODO move everything into BackendDelegate and dissolve.
object Gradle:
  def getConfiguration(project: Project, name: String): Configuration = project
    .getConfigurations
    .getByName(name)

  def createConfiguration(project: Project, name: String, description: String): Configuration =
    val result: Configuration = project.getConfigurations.create(name)
    result.setVisible(false)
    result.setCanBeConsumed(false)
    result.setDescription(description)
    result

  def getSourceSets(project: Project): SourceSetContainer = project
    .getExtensions
    .getByType(classOf[JavaPluginExtension])
    .getSourceSets

  def getSourceSet(project: Project, name: String): SourceSet = getSourceSets(project).getByName(name)

  def getClassesTask(project: Project, sourceSet: SourceSet): Task = project
    .getTasks
    .getByName(sourceSet.getClassesTaskName)
  
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
    