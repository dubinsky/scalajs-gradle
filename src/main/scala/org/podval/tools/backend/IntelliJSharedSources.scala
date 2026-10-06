package org.podval.tools.backend

import org.gradle.api.{Action, Project, Task}
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.DefaultSourceDirectorySet
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.{AbstractCopyTask, SourceTask}
import org.podval.tools.util.{Configurations, Reflection, Tasks}
import scala.jdk.CollectionConverters.ListHasAsScala
import java.io.File

// IntelliJ imports every Gradle source directory as a content root.
// Adding a shared sibling's directories during configuration makes those roots appear twice.
// The directories are attached in doFirst, after import, and the add is idempotent.
object IntelliJSharedSources:
  def directories(directorySet: SourceDirectorySet): Seq[File] = Reflection
    .Get[java.util.List[Object], DefaultSourceDirectorySet]("source")(directorySet)
    .asScala
    .toSeq
    .filter(_.isInstanceOf[File])
    .map(_.asInstanceOf[File])

  def defer(project: Project, shared: Project, add: () => Unit): Unit =
    Configurations.addDependency(project, Configurations.implementationName(project), shared)

    Set(
      classOf[SourceTask],
      classOf[AbstractArchiveTask],
      classOf[AbstractCopyTask]
    )
      .foreach(Tasks.configureEach(
        project,
        _,
        // The task action must be an Action, not a lambda.
        _.doFirst(new Action[Task]:
          override def execute(task: Task): Unit = add()
        )
      ))
