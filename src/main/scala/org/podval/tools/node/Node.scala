package org.podval.tools.node

import org.podval.tools.build.{Runner, Version}
import java.io.File

final class Node(
  val node: File,
  val npm: File
):
  override def toString: String = s"Node v$version with root $root"

  def nodeProject(root: File, runner: Runner): NodeProject = NodeProject(node = this, root, runner)

  // If installation was not installed from a distribution, root is meaningless.
  def root: File =
    val result: File = node.getParentFile
    if result.getName == "bin" then result.getParentFile else result

  def bin: File = node.getParentFile
  
  lazy val version: Version =
    if !node.exists
    then Version("0.0.0")
    // version printed by `node` starts with "v"
    else Version(Exec(s"$node -v").drop(1))
