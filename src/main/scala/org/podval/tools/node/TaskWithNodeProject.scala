package org.podval.tools.node

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.podval.tools.build.Version
import org.podval.tools.gradle.Tasks
import org.podval.tools.task.{TaskWithDependencyInstallable, TaskWithRunner}
import java.io.File

object TaskWithNodeProject:
  def configureTasks(
    project: Project,
    version: Option[Version],
    nodeProjectRoot: File
  ): Unit = Tasks.configureEach(
    project,
    classOf[TaskWithNodeProject],
    (task: TaskWithNodeProject) =>
      task.getVersion.set(version.map(_.toString).orNull)
      task.getNodeProjectRoot.set(nodeProjectRoot)
    )
    
trait TaskWithNodeProject extends TaskWithDependencyInstallable[Node] with TaskWithRunner:
  final override protected def dependencyInstallable: NodeDependency.type = NodeDependency

  @Internal def getNodeProjectRoot: Property[File]
  
  final def nodeProject: NodeProject = dependencyInstalled
    .nodeProject(
      root = getNodeProjectRoot.get,
      runner = runner
    )
