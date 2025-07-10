package org.podval.tools.node

import org.gradle.api.Project
import org.gradle.api.provider.{ListProperty, Property}
import org.gradle.process.ExecOperations
import org.podval.tools.platform.Runner
import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava}
import java.io.File
import javax.inject.Inject

object NodeExtension:
  private def nodeProjectRoot(project: Project): File = project.getProjectDir

abstract class NodeExtension @Inject(project: Project, execOperations: ExecOperations):
  def getVersion: Property[String]

  def getModules: ListProperty[String]
  getModules.convention(List.empty.asJava)
  
  // Set properties needed to run Node on the `TaskWithNodeProject`s.
  project
    .getTasks
    .withType(classOf[TaskWithNodeProject])
    .configureEach: (task: TaskWithNodeProject) =>
      task.getVersion.set(getVersion)
      task.getNodeProjectRoot.set(NodeExtension.nodeProjectRoot(project))

  // Add the utility tasks.
  project.getTasks.register("npm" , classOf[NodeTask.NpmRunTask])
  project.getTasks.register("node", classOf[NodeTask.NodeRunTask])

  // install Node (if needed) and set up Node project (if needed).
  project.afterEvaluate: (project: Project) =>
    NodeDependency
      .getInstalledOrInstall(
        version = Option(getVersion.getOrNull),
        project = project
      )
      .nodeProject(
        root = NodeExtension.nodeProjectRoot(project),
        runner = Runner(
          execOperations = execOperations,
          logger = project.getLogger
        )
      )
      .setUp(
        installModules = getModules.get.asScala.toList
      )
