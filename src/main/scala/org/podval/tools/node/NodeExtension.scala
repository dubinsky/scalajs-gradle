package org.podval.tools.node

import org.gradle.api.Project
import org.gradle.api.provider.{ListProperty, Property}
import org.gradle.api.tasks.TaskProvider
import org.podval.tools.build.Version
import org.podval.tools.util.{Extensions, Projects, Tasks}
import scala.jdk.CollectionConverters.SeqHasAsJava
import java.io.File
import javax.inject.Inject

object NodeExtension:
  def create(project: Project): NodeExtension = Extensions.create(project, "node", classOf[NodeExtension])

  private def nodeProjectRoot(project: Project): File = Projects.projectDir(project)

// Note: Gradle extensions must be abstract.
abstract class NodeExtension @Inject(project: Project):
  def getVersion: Property[String]
  private def version: Option[Version] = Version(getVersion)

  def getModules: ListProperty[String]
  getModules.convention(List.empty.asJava)

  private val setup: TaskProvider[NodeSetupTask] = Tasks.register(
    project,
    classOf[NodeSetupTask],
    "nodeSetup",
    "Installs Node.js and the npm modules requested by the node extension.",
    Tasks.buildGroup
  )

  project.getTasks.withType(classOf[NodeProjectTask]).configureEach: (task: NodeProjectTask) =>
    if task.getName != setup.getName then task.dependsOn(setup)

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

  // Copy the extension values onto the tasks. Installation runs in nodeSetup.
  project.afterEvaluate: (project: Project) =>
    NodeProjectTask.configureTasks(project, version, NodeExtension.nodeProjectRoot(project))
    setup.configure(_.getModules.set(getModules))
