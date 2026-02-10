package org.podval.tools.node

import org.gradle.api.logging.{Logger, Logging}
import org.gradle.process.ExecSpec
import org.podval.tools.build.Runner
import scala.jdk.CollectionConverters.SeqHasAsJava
import java.io.File

object NodeProject:
  private val logger: Logger = Logging.getLogger(getClass)
  
final class NodeProject(
  val node: Node,
  root: File,
  runner: Runner
):
  override def toString: String = s"Node project in $root with $node"
  private def nodeModules: File = File(root, "node_modules")
  private def nodePathEnvironmentVariable: (String, String) = "NODE_PATH" -> nodeModules.getAbsolutePath
  private def pathEnvironmentVariable: (String, String) = "PATH" -> s"${node.bin.getAbsolutePath}:${System.getenv("PATH")}"
  def nodeEnv: Seq[(String, String)] = Seq(nodePathEnvironmentVariable)

  def setUp(installModules: List[String]): Unit =
    val isSetUp: Boolean = File(root, "package.json").exists

    // Initialize Node project
    if !isSetUp then
      NodeProject.logger.lifecycle(s"Setting up $this")
      npm(List("init", "private"), log = !isSetUp)

    // Install Node modules
    nodeModules.mkdirs()
    npm(List("install") ++ installModules, log = !isSetUp)

  def node(arguments: List[String], log: Boolean): Unit = run(
    command = node.node,
    environment = nodeEnv,
    cwd = None,
    arguments,
    log
  )

  def npm(arguments: List[String], log: Boolean): Unit = run(
    command = node.npm,
    environment = Seq(nodePathEnvironmentVariable, pathEnvironmentVariable),
    // in local mode, npm puts packages into node_modules under the current working directory
    cwd = Some(root),
    arguments,
    log
  )

  private def run(
    command: File,
    environment: Seq[(String, String)],
    cwd: Option[File],
    arguments: List[String],
    log: Boolean
  ): Unit = runner.exec(log, (execSpec: ExecSpec) =>
    execSpec.setCommandLine((List(command.getAbsolutePath) ++ arguments).asJava)
    environment.foreach((name, value) => execSpec.environment(name, value))
    cwd.foreach(execSpec.setWorkingDir)
  )
