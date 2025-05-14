package org.podval.tools.node

import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.{ListProperty, Property}
import org.gradle.process.ExecOperations
import org.podval.tools.build.{CreateExtension, Gradle, GradleBuildContext}
import scala.jdk.CollectionConverters.{ListHasAsScala, SetHasAsScala}
import java.io.File
import javax.inject.Inject

object NodeExtension:
  private def nodeModulesParent(project: Project): File = project.getProjectDir

  def create(configure: NodeExtension => Unit): CreateExtension[NodeExtension] = CreateExtension[NodeExtension](
    name = "node",
    clazz = classOf[NodeExtension],
    configure = configure
  )

abstract class NodeExtension @Inject(project: Project, execOperations: ExecOperations):
  def getVersion: Property[String]

  def getModules: ListProperty[String]

  // Set properties needed to run Node on the `TaskWithNode`s.
  project.getTasks.withType(classOf[TaskWithNode]).configureEach: (taskWithNode: TaskWithNode) =>
    taskWithNode.getVersion.set(getVersion)
    taskWithNode.getGradleUserHomeDir.set(project.getGradle.getGradleUserHomeDir)
    taskWithNode.getNodeModulesParent.set(NodeExtension.nodeModulesParent(project))

  // Add the utility tasks.
  project.getTasks.register("npm" , classOf[NodeTask.NpmRunTask])
  project.getTasks.register("node", classOf[NodeTask.NodeRunTask])

  project.afterEvaluate: (project: Project) =>
    val context: GradleBuildContext = GradleBuildContext(project, execOperations)

    // install Node (if needed) and set up Node project (if needed).
    NodeDependency
      .getInstalledOrInstall(
        version = Gradle.toOption(getVersion),
        context = context
      )
      .node(
        nodeModulesParent = NodeExtension.nodeModulesParent(project)
      )
      .setUpNodeProject(
        installModules = getModules.get.asScala.toList,
        logInfo = project.getLogger.log(LogLevel.INFO, _),
        logLifecycle = project.getLogger.log(LogLevel.LIFECYCLE, _)
      )
