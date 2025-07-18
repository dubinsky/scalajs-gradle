package org.podval.tools.node

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.{Input, Internal, Optional}
import org.podval.tools.gradle.{TaskWithGradleUserHomeDir, TaskWithRunner, Tasks}
import java.io.File

object TaskWithNodeProject:
  def configureTasks(
    project: Project,
    version: Option[String],
    nodeProjectRoot: File
  ): Unit =
    Tasks.configureEach(project, classOf[TaskWithNodeProject], (task: TaskWithNodeProject) =>
      task.getVersion.set(version.orNull)
      task.getNodeProjectRoot.set(nodeProjectRoot)
    )
    
trait TaskWithNodeProject extends TaskWithRunner with TaskWithGradleUserHomeDir:
  @Optional @Input def getVersion: Property[String]
  @Internal def getNodeProjectRoot: Property[File]

  final def nodeProject: NodeProject = NodeDependency
    .getInstalled(
      version = Option(getVersion.getOrNull),
      gradleUserHomeDir = getGradleUserHomeDir.get
    )
    .nodeProject(
      root = getNodeProjectRoot.get,
      runner = runner
    )
