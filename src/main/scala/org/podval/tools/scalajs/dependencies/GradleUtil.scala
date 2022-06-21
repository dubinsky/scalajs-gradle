package org.podval.tools.scalajs.dependencies

import org.gradle.api.artifacts.Configuration
import org.gradle.api.{Project, Task}
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.tasks.{SourceSet, TaskProvider}
import org.opentorah.build.Gradle.*
import scala.jdk.CollectionConverters.*

// TODO merge into org.opentorah.build.Gradle
object GradleUtil:
  def getConfiguration(project: Project, name: String): Configuration =
    project.getConfigurations.getByName(name)

  def getScalaCompile(classesTask: Task): ScalaCompile = classesTask
    .getDependsOn
    .asScala
    .find(classOf[TaskProvider[ScalaCompile]].isInstance)
    .get
    .asInstanceOf[TaskProvider[ScalaCompile]]
    .get

  def getScalaCompile(project: Project, sourceSet: SourceSet): ScalaCompile =
    getScalaCompile(project.getClassesTask(sourceSet))
