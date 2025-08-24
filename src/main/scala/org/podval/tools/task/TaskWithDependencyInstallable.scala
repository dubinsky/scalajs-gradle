package org.podval.tools.task

import org.gradle.api.provider.Property
import org.gradle.api.tasks.{Input, Internal, Optional}
import org.gradle.api.Project
import org.podval.tools.build.{DependencyInstallable, Version}
import org.podval.tools.gradle.{Projects, Tasks}
import java.io.File

trait TaskWithDependencyInstallable[T] extends TaskWithOutput:
  protected def dependencyInstallable: DependencyInstallable[T]

  @Internal def getGradleUserHomeDir: Property[File]

  @Optional @Input def getVersion: Property[String]

  final protected def dependencyInstalled: T = dependencyInstallable
    .getInstalled(
      version = Version(getVersion),
      gradleUserHomeDir = getGradleUserHomeDir.get,
      output = output
    )

object TaskWithDependencyInstallable:
  def configureTasks(project: Project): Unit = Tasks.configureEach(
    project,
    classOf[TaskWithDependencyInstallable[?]],
    _.getGradleUserHomeDir.set(Projects.gradleUserHomeDir(project))
  )
