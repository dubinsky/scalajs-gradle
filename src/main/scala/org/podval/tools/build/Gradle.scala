package org.podval.tools.build

import org.gradle.api.{Project, Task}
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.tasks.{SourceSet, TaskProvider}
import org.gradle.api.tasks.scala.ScalaCompile
import scala.jdk.CollectionConverters.SetHasAsScala

object Gradle:

  extension(project: Project)
    def getConfiguration(name: String): Configuration = project
      .getConfigurations
      .getByName(name)

    def getSourceSet(name: String): SourceSet = project
      .getExtensions
      .getByType(classOf[JavaPluginExtension])
      .getSourceSets
      .getByName(name)

    def getClassesTask(sourceSet: SourceSet): Task = project
      .getTasks
      .getByName(sourceSet.getClassesTaskName)
    
    def getScalaCompile(sourceSet: SourceSet): ScalaCompile = project
      .getClassesTask(sourceSet)
      .getDependsOn
      .asScala
      .find(classOf[TaskProvider[ScalaCompile]].isInstance)
      .get
      .asInstanceOf[TaskProvider[ScalaCompile]]
      .get

  extension[T](property: Property[T])
    def toOption: Option[T] =
      if !property.isPresent then None else Some(property.get)

  extension[T](property: Property[String])
    def byName(default: => T, all: List[T]): T =
      if !property.isPresent then default else all.find(_.toString == property.get).get
