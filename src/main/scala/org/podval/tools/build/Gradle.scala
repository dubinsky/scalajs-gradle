package org.podval.tools.build

import org.gradle.api.{Project, Task}
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.tasks.SourceSet

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
  
  def toOption[T](property: Property[T]): Option[T] =
    if !property.isPresent then None else Some(property.get)
    