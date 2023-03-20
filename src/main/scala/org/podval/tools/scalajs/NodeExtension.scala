package org.podval.tools.scalajs

import org.gradle.api.{DefaultTask, Project}
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.{ListProperty, Property}
import org.gradle.api.tasks.{Input, TaskAction}
import org.opentorah.build.Gradle.*
import org.opentorah.build.GradleBuildContext
import org.opentorah.node.{Node, NodeDependency, NodeInstallation}
import org.opentorah.platform.Exec
import java.io.File
import javax.inject.Inject

// TODO switch to org.opentorah.node.NodeExtension once it is released
abstract class NodeExtension @Inject(project: Project):
  def getVersion: Property[String]
  def version: Option[String] = getVersion.toOption
  
  def getModules: ListProperty[String]
  def modules: List[String] = getModules.toList

  def node: Node = node(installIfDoesNotExist = false)

  def node(installIfDoesNotExist: Boolean): Node =
    val installation: NodeInstallation = version match
      case None =>
        NodeInstallation.fromOs.get
      case Some(version) =>
        NodeDependency(version).getInstallation(
          GradleBuildContext(project),
          installIfDoesNotExist = installIfDoesNotExist,
          mustExist = true
        )

    installation.getNode(nodeModulesParent = project.getProjectDir)

  def log(project: Project, logLevel: LogLevel): String => Unit =
    (message: String) => project.getLogger.log(logLevel, message)

  def node(arguments: String): Unit = node(arguments, LogLevel.LIFECYCLE)
  def node(arguments: String, logLevel: LogLevel): Unit = runNode(node, arguments, log(project, logLevel))
  private def runNode(node: Node, arguments: String, log: String => Unit): String = run(
    command = node.installation.nodeExec,
    commandName = "node",
    arguments,
    cwd = None,
    extraEnv = Seq(node.nodeEnv),
    log
  )

  def npm(arguments: String): Unit = npm(arguments, LogLevel.LIFECYCLE)
  def npm(arguments: String, logLevel: LogLevel): Unit = runNpm(node, arguments, log(project, logLevel))
  private def runNpm(node: Node, arguments: String, log: String => Unit): String = run(
    command = node.installation.npmExec,
    commandName = "npm",
    arguments,
    // in local mode, npm puts packages into node_modules under the current working directory
    cwd = Some(node.nodeModulesParent),
    // TODO do I need the system path here?
    extraEnv = Seq(node.nodeEnv) ++ Seq(("PATH", node.installation.getBin.getAbsolutePath + ":" + System.getenv("PATH"))),
    log
  )

  private def run(
    command: File,
    commandName: String,
    arguments: String,
    cwd: Option[File],
    extraEnv: Seq[(String, String)],
    log: String => Unit
  ): String =
    log(s"Running '$commandName $arguments'...")
    val output: String = Exec(
      command,
      arguments,
      cwd,
      extraEnv*
    )
    log(s"Output: [$output]\n")
    output

  final def setUpNodeProject(requiredModules: List[String]): Unit =
    val nodeExtension: NodeExtension = NodeExtension.get(project)
    val node: Node = nodeExtension.node

    val isProjectSetUp: Boolean = File(project.getProjectDir, "package.json").exists

    // Initialize Node project
    if !isProjectSetUp then nodeExtension.npm(arguments = "init private")

    // Install Node modules
    node.nodeModules.mkdirs()
    nodeExtension.npm(
      arguments = "install " + (requiredModules ++ nodeExtension.modules).mkString(" "),
      logLevel = if isProjectSetUp then LogLevel.INFO else LogLevel.LIFECYCLE
    )

object NodeExtension:
  def add(project: Project): Unit =
    project.getExtensions.create("node", classOf[NodeExtension])
    project.getTasks.create("npm" , classOf[NpmTask])
    project.getTasks.create("node", classOf[NodeTask])
    
  def get(project: Project): NodeExtension = project.getExtensions.getByType(classOf[NodeExtension])

  class NodeTask extends DefaultTask:
    setGroup("other")
    setDescription("Run commands with 'node'")
    
    private var arguments: String = ""
    @TaskAction def execute(): Unit = get(getProject).node(arguments)
    @Input def getArguments: String = arguments
    
    @org.gradle.api.tasks.options.Option(option = "node-arguments", description = "The command to execute with 'node'")
    def setArguments(value: String): Unit = arguments = value

  class NpmTask extends DefaultTask:
    setGroup("other")
    setDescription("Run commands with 'npm'")
    
    private var arguments: String = ""
    @TaskAction def execute(): Unit = get(getProject).npm(arguments)
    @Input def getArguments: String = arguments
    
    @org.gradle.api.tasks.options.Option(option = "npm-arguments", description = "The command to execute with 'npm'")
    def setArguments(value: String): Unit = arguments = value
