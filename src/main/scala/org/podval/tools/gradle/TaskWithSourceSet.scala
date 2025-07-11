package org.podval.tools.gradle

import org.gradle.api.{Project, Task}
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.{InputFiles, Internal, SourceSet}
import scala.jdk.CollectionConverters.SetHasAsScala
import java.io.File

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
