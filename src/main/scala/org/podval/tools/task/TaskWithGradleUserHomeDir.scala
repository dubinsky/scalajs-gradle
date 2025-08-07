package org.podval.tools.task

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.{Project, Task}
import org.podval.tools.gradle.{Projects, Tasks}
import java.io.File

trait TaskWithGradleUserHomeDir extends Task:
  @Internal def getGradleUserHomeDir: Property[File]

object TaskWithGradleUserHomeDir:
  def configureTasks(project: Project): Unit = Tasks.configureEach(
    project,
    classOf[TaskWithGradleUserHomeDir],
    _.getGradleUserHomeDir.set(Projects.gradleUserHomeDir(project))
  )
