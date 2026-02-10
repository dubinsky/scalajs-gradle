package org.podval.tools.node

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.{Input, Internal, Optional}
import org.podval.tools.build.{RunnerTask, Version}
import org.podval.tools.util.{Projects, Tasks}
import java.io.File

trait NodeProjectTask extends RunnerTask:
  @Internal def getGradleUserHomeDir: Property[File]

  @Optional @Input def getVersion: Property[String]

  @Internal def getNodeProjectRoot: Property[File]

  final def nodeProject: NodeProject = NodeInstaller
    .getInstalled(
      version = Version(getVersion),
      gradleUserHomeDir = getGradleUserHomeDir.get,
      output = output
    )
    .nodeProject(
      root = getNodeProjectRoot.get,
      runner = runner
    )

object NodeProjectTask:
  def configureTasks(
    project: Project,
    version: Option[Version],
    nodeProjectRoot: File
  ): Unit =
    val gradleUserHomeDir: File = Projects.gradleUserHomeDir(project)
    val versionEffective: String = version.map(_.toString).orNull

    Tasks.configureEach(
      project,
      classOf[NodeProjectTask],
      (task: NodeProjectTask) =>
        task.getGradleUserHomeDir.set(gradleUserHomeDir)
        task.getVersion.set(versionEffective)
        task.getNodeProjectRoot.set(nodeProjectRoot)
    )

