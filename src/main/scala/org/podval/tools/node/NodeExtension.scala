package org.podval.tools.node

import org.gradle.api.Project
import org.gradle.api.provider.{ListProperty, Property}
import org.gradle.api.tasks.TaskProvider
import org.gradle.process.ExecOperations
import org.podval.tools.gradle.{Projects, Tasks}
import org.podval.tools.platform.Runner
import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava}
import java.io.File
import javax.inject.Inject

object NodeExtension:
  private def nodeProjectRoot(project: Project): File = Projects.projectDir(project)

abstract class NodeExtension @Inject(project: Project, execOperations: ExecOperations):
  def getVersion: Property[String]
  final def version: Option[String] = Option(getVersion.getOrNull)

  def getModules: ListProperty[String]
  getModules.convention(List.empty.asJava)
  final def modules: List[String] = getModules.get.asScala.toList
  
  // Set properties needed to run Node on the `TaskWithNodeProject`s.
  Tasks.configureEach(project, classOf[TaskWithNodeProject], (task: TaskWithNodeProject) =>
    task.getVersion.set(getVersion)
    task.getNodeProjectRoot.set(NodeExtension.nodeProjectRoot(project))
  )

  // Add the utility tasks.
  private def register[T <: NodeTask](
    commandName: String,
    taskClass: Class[T]
  ): TaskProvider[T] = Tasks.register(
    project,
    taskClass,
    commandName,
    s"Runs command supplied with the command line option '--$commandName-arguments' using '$commandName'.",
    Tasks.otherGroup
  )
  register("node", classOf[NodeTask.NodeRunTask])
  register("npm" , classOf[NodeTask.NpmRunTask ])

  // install Node (if needed) and set up Node project (if needed).
  project.afterEvaluate: (project: Project) =>
    NodeDependency
      .getInstalledOrInstall(
        version = version,
        project = project
      )
      .nodeProject(
        root = NodeExtension.nodeProjectRoot(project),
        runner = Runner(execOperations)
      )
      .setUp(
        installModules = modules
      )
