package org.podval.tools.node

import org.gradle.process.ExecSpec
import org.podval.tools.platform.{Exec, Runner}
import org.slf4j.{Logger, LoggerFactory}
import scala.jdk.CollectionConverters.SeqHasAsJava
import java.io.File

object NodeProject:
  private val logger: Logger = LoggerFactory.getLogger(NodeProject.getClass)
  
final class NodeProject(
  val node: Node,
  root: File,
  runner: Runner
):
  override def toString: String = s"Node project in $root with $node"
  private val nodeModules: File = File(root, "node_modules")
  val nodeEnv: Seq[(String, String)] = Seq(("NODE_PATH", nodeModules.getAbsolutePath))
  val npmEnv: Seq[(String, String)] = nodeEnv ++ Seq(("PATH", s"${node.bin.getAbsolutePath}:${System.getenv("PATH")}"))

  def setUp(installModules: List[String]): Unit =
    val isSetUp: Boolean = File(root, "package.json").exists

    // Initialize Node project
    if !isSetUp then
      NodeProject.logger.warn(s"Setting up $this")
      npm(List("init", "private"))

    // Install Node modules
    nodeModules.mkdirs()
    npm(List("install") ++ installModules)

  def node(arguments: List[String]): Unit = run(
    command = node.node,
    environment = nodeEnv,
    cwd = None,
    arguments
  )

  def npm(arguments: List[String]): Unit = run(
    command = node.npm,
    environment = npmEnv,
    // in local mode, npm puts packages into node_modules under the current working directory
    cwd = Some(root),
    arguments
  )

  private def run(
    command: File,
    environment: Seq[(String, String)],
    cwd: Option[File],
    arguments: List[String]
  ): Unit = runner.exec((execSpec: ExecSpec) =>
    execSpec.setCommandLine((List(command.getAbsolutePath) ++ arguments).asJava)
    environment.foreach((name, value) => execSpec.environment(name, value))
    cwd.foreach(execSpec.setWorkingDir)
  )
