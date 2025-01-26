package org.podval.tools.node

import java.io.File

final class NodeInstallation(
  val node: File,
  val npm: File
):
  override def toString: String = s"Node installation with root $root"

  // TODO get the version of node

  // Note: if installation was not installed from the distribution, root is meaningless
  def root: File =
    val result: File = node.getParentFile
    if result.getName == "bin" then result.getParentFile else result

  def bin: File = node.getParentFile

  def node(nodeModulesParent: File): Node = Node(this, nodeModulesParent)
