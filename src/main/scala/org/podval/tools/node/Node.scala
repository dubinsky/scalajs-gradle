package org.podval.tools.node

import org.podval.tools.platform.Exec
import java.io.File

final class Node(
  val installation: NodeInstallation,
  val nodeModulesParent: File
):
  override def toString: String = s"Node with modules in $nodeModules and $installation"

  val nodeModules: File = File(nodeModulesParent, "node_modules")

  def mkNodeModules(): Unit = nodeModules.mkdirs()
  
  val nodeEnv: Seq[(String, String)] = Seq(("NODE_PATH", nodeModules.getAbsolutePath))

  def node(arguments: String, log: String => Unit): String = run(
    command = installation.node,
    commandName = "node",
    arguments,
    cwd = None,
    extraEnv = nodeEnv,
    log
  )

  def npm(arguments: String, log: String => Unit): String = run(
    command = installation.npm,
    commandName = "npm",
    arguments,
    // in local mode, npm puts packages into node_modules under the current working directory
    cwd = Some(nodeModulesParent),
    // TODO do I need the system path here?
    extraEnv = nodeEnv ++ Seq(("PATH", installation.bin.getAbsolutePath + ":" + System.getenv("PATH"))),
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
