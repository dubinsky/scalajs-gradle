package org.podval.tools.node

import org.podval.tools.platform.Exec
import java.io.File

final class NodeInstallation(
  val node: File,
  val npm: File
):
  override def toString: String = s"Node installation for version $nodeVersion with root $root"

  // Note: if installation was not installed from the distribution, root is meaningless
  def root: File =
    val result: File = node.getParentFile
    if result.getName == "bin" then result.getParentFile else result

  def bin: File = node.getParentFile

  def node(nodeModulesParent: File): Node = Node(this, nodeModulesParent)

  private def nodeVersion: String =
    if !node.exists then "<does not exist>" else Exec(
      command = node,
      args = "-v",
      cwd = None
    ).drop(1) // version printed by `node` starts with "v"...

  def node(
    arguments: String,
    extraEnv: Seq[(String, String)],
    log: String => Unit
  ): String = run(
    command = node,
    commandName = "node",
    arguments,
    cwd = None,
    extraEnv,
    log
  )

  def npm(
    arguments: String,
    cwd: Option[File],
    extraEnv: Seq[(String, String)],
    log: String => Unit
  ): String = run(
    command = npm,
    commandName = "npm",
    arguments,
    cwd,
    extraEnv,
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
