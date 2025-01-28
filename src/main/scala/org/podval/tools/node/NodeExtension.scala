package org.podval.tools.node

import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.{ListProperty, Property}
import org.gradle.process.ExecOperations
import org.podval.tools.build.GradleBuildContext
import scala.jdk.CollectionConverters.{ListHasAsScala, SetHasAsScala}
import java.io.File
import javax.inject.Inject

abstract class NodeExtension @Inject(project: Project, execOperations: ExecOperations):
  def getVersion: Property[String]

  def getModules: ListProperty[String]

  // add utility tasks
  project.getTasks.register("npm", classOf[NodeTask.NpmRunTask])
  project.getTasks.register("node", classOf[NodeTask.NodeRunTask])

  project.afterEvaluate((project: Project) =>
    val context: GradleBuildContext = GradleBuildContext(project, execOperations)
    def nodeModulesParent: File = project.getProjectDir

    // install Node (if needed) and set up Node project (if needed).
    NodeDependency
      .getInstalledOrInstall(
        version = NodeExtension.toOption(getVersion),
        context = context
      )
      .node(
        nodeModulesParent = nodeModulesParent
      )
      .setUpNodeProject(
        installModules = getModules.get.asScala.toList,
        logInfo = project.getLogger.log(LogLevel.INFO, _),
        logLifecycle = project.getLogger.log(LogLevel.LIFECYCLE, _)
      )

    // set properties needed to run Node on the `TaskWithNode`s.
    project.getTasks.asScala.foreach {
      case taskWithNode: TaskWithNode =>
        taskWithNode.getVersion.set(getVersion)
        taskWithNode.getGradleUserHomeDir.set(context.gradleUserHomeDir)
        taskWithNode.getNodeModulesParent.set(nodeModulesParent)
      case _ =>
    }
  )

object NodeExtension:
  def addTo(project: Project): NodeExtension = project.getExtensions.create("node", classOf[NodeExtension])

  def toOption[T](property: Property[T]): Option[T] =
    if !property.isPresent then None else Some(property.get)
