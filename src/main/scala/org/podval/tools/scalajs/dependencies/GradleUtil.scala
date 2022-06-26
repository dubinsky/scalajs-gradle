package org.podval.tools.scalajs.dependencies

import org.gradle.api.artifacts.Configuration
import org.gradle.api.{Project, Task}
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.tasks.{SourceSet, TaskProvider}
import org.opentorah.build.Gradle.*
import scala.jdk.CollectionConverters.*

// TODO use from org.opentorah.build.Gradle when released
object GradleUtil:
  extension (classesTask: Task)
    def getScalaCompile: ScalaCompile = classesTask
      .getDependsOn
      .asScala
      .find(classOf[TaskProvider[ScalaCompile]].isInstance)
      .get
      .asInstanceOf[TaskProvider[ScalaCompile]]
      .get

  extension (project: Project)
    def getConfiguration(name: String): Configuration = project.getConfigurations.getByName(name)
    def getScalaCompile(sourceSet: SourceSet): ScalaCompile = project.getClassesTask(sourceSet).getScalaCompile
