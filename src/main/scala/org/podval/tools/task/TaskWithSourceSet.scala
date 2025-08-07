package org.podval.tools.task

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.{InputFiles, Internal, SourceSet}
import org.gradle.api.{Project, Task}
import org.podval.tools.gradle.{Configurations, Tasks}
import java.io.File
import scala.jdk.CollectionConverters.SetHasAsScala

trait TaskWithSourceSet extends Task:
  @Internal def isTest: Boolean

  @InputFiles def getRuntimeClasspath: ConfigurableFileCollection
  final def runtimeClasspath: Seq[File] = getRuntimeClasspath.getFiles.asScala.toSeq

object TaskWithSourceSet:
  def configureTasks(project: Project): Unit = Tasks.configureEach(
    project,
    classOf[TaskWithSourceSet],
    (task: TaskWithSourceSet) =>
      val sourceSet: SourceSet = Configurations.sourceSet(project, task.isTest)
      task.dependsOn(project.getTasks.named(sourceSet.getClassesTaskName))
      task.getRuntimeClasspath.setFrom(sourceSet.getRuntimeClasspath)
    )
