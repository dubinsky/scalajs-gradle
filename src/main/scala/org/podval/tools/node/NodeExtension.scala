package org.podval.tools.node

import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.{ListProperty, Property}
import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava, SetHasAsScala}
import java.io.File
import javax.inject.Inject

object NodeExtension:
  private def nodeModulesParent(project: Project): File = project.getProjectDir

abstract class NodeExtension @Inject(project: Project):
  def getVersion: Property[String]

  def getModules: ListProperty[String]
  getModules.convention(List.empty.asJava)
  
  // Set properties needed to run Node on the `TaskWithNode`s.
  project.getTasks.withType(classOf[TaskWithNode]).configureEach: (taskWithNode: TaskWithNode) =>
    taskWithNode.getVersion.set(getVersion)
    taskWithNode.getGradleUserHomeDir.set(project.getGradle.getGradleUserHomeDir)
    taskWithNode.getNodeModulesParent.set(NodeExtension.nodeModulesParent(project))

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
      .node(
        nodeModulesParent = NodeExtension.nodeModulesParent(project)
      )
      .setUpNodeProject(
        installModules = getModules.get.asScala.toList,
        logInfo = project.getLogger.log(LogLevel.INFO, _),
        logLifecycle = project.getLogger.log(LogLevel.LIFECYCLE, _)
      )
