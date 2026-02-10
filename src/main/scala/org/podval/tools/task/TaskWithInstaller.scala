package org.podval.tools.task

import org.gradle.api.provider.Property
import org.gradle.api.tasks.{Input, Internal, Optional}
import org.gradle.api.Project
import org.podval.tools.build.{Installer, Version}
import org.podval.tools.gradle.{Projects, Tasks}
import java.io.File

trait TaskWithInstaller[T] extends TaskWithOutput:
  protected def installer: Installer[T]

  @Internal def getGradleUserHomeDir: Property[File]

  @Optional @Input def getVersion: Property[String]

  final protected def installation: T = installer
    .getInstalled(
      version = Version(getVersion),
      gradleUserHomeDir = getGradleUserHomeDir.get,
      output = output
    )

object TaskWithInstaller:
  def configureTasks(project: Project): Unit = Tasks.configureEach(
    project,
    classOf[TaskWithInstaller[?]],
    _.getGradleUserHomeDir.set(Projects.gradleUserHomeDir(project))
  )
