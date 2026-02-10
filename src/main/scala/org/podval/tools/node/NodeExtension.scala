package org.podval.tools.node

import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.{ListProperty, Property}
import org.gradle.api.tasks.TaskProvider
import org.gradle.process.ExecOperations
import org.podval.tools.build.{Output, Runner, Version}
import org.podval.tools.util.{Extensions, Projects, Tasks}
import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava}
import java.io.File
import javax.inject.Inject

object NodeExtension:
  def create(project: Project): NodeExtension = Extensions.create(project, "node", classOf[NodeExtension])

  private def nodeProjectRoot(project: Project): File = Projects.projectDir(project)

// Note: Gradle extensions must be abstract.
abstract class NodeExtension @Inject(project: Project, execOperations: ExecOperations):
  def getVersion: Property[String]
  private def version: Option[Version] = Version(getVersion)

  def getModules: ListProperty[String]
  getModules.convention(List.empty.asJava)
  private def modules: List[String] = getModules.get.asScala.toList

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

  // configure tasks, install Node (if needed) and set up Node project (if needed).
  project.afterEvaluate: (project: Project) =>
    NodeProjectTask.configureTasks(project, version, NodeExtension.nodeProjectRoot(project))

    val output: Output = Output(
      logLevelEnabled = LogLevel.LIFECYCLE,
      isRunningInIntelliJ = false,
      logSource = "Node.js extension"
    )
    NodeInstaller
      .getInstalledOrInstall(
        version = version,
        project = project,
        output = output
      )
      .nodeProject(
        root = NodeExtension.nodeProjectRoot(project),
        runner = Runner(
          execOperations,
          output
        )
      )
      .setUp(
        installModules = modules
      )
