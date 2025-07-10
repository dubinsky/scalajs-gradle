package org.podval.tools.platform

import org.gradle.api.{Project, Task}
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.podval.tools.build.DependencyInstallable
import java.io.File

trait TaskWithGradleUserHomeDir extends Task:
  @Internal def getGradleUserHomeDir: Property[File]

object TaskWithGradleUserHomeDir:
  def configureTasks(project: Project): Unit = project
    .getTasks
    .withType(classOf[TaskWithGradleUserHomeDir])
    .configureEach: (task: TaskWithGradleUserHomeDir) =>
      task.getGradleUserHomeDir.set(DependencyInstallable.getGradleUserHomeDir(project))
