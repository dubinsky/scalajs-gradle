package org.podval.tools.node

import org.gradle.api.Project
import org.gradle.api.provider.{ListProperty, Property}
import org.gradle.process.ExecOperations
import org.podval.tools.build.Gradle.*
import org.podval.tools.build.GradleBuildContext
import javax.inject.Inject

abstract class NodeExtension @Inject(project: Project, execOperations: ExecOperations):
  def getVersion: Property[String]

  def getModules: ListProperty[String]

  def ensureNodeIsInstalled(): Unit = NodeDependency.getInstalledOrInstall(
    version = getVersion.toOption,
    context = GradleBuildContext(project, execOperations)
  )

object NodeExtension:
  def addTo(project: Project): Unit =
    project.getExtensions.create("node", classOf[NodeExtension])
    project.getTasks.register("npm" , classOf[NodeTask.NpmRunTask])
    project.getTasks.register("node", classOf[NodeTask.NodeRunTask])

  def get(project: Project): NodeExtension = project.getExtensions.getByType(classOf[NodeExtension])
