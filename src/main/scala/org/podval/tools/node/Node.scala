package org.podval.tools.node

import java.io.File

final class Node(
  val installation: NodeInstallation,
  val nodeModulesParent: File
):
  override def toString: String = s"Node with modules in $nodeModules and $installation"

  private val nodeModules: File = File(nodeModulesParent, "node_modules")
  
  val nodeEnv: Seq[(String, String)] = Seq(("NODE_PATH", nodeModules.getAbsolutePath))

  def node(arguments: String, log: String => Unit): String = installation.node(
    arguments,
    extraEnv = nodeEnv,
    log
  )

  def npm(arguments: String, log: String => Unit): String = installation.npm(
    arguments,
    // in local mode, npm puts packages into node_modules under the current working directory
    cwd = Some(nodeModulesParent),
    // TODO do I need the system path here?
    extraEnv = nodeEnv ++ Seq(("PATH", installation.bin.getAbsolutePath + ":" + System.getenv("PATH"))),
    log
  )

  def setUpNodeProject(
    installModules: List[String],
    logInfo: String => Unit,
    logLifecycle: String => Unit,
  ): Unit =
    val isProjectSetUp: Boolean = File(nodeModulesParent, "package.json").exists

    // Initialize Node project
    if !isProjectSetUp then npm(arguments = "init private", logLifecycle)

    // Install Node modules
    nodeModules.mkdirs()
    npm(
      arguments = "install " + installModules.mkString(" "),
      log = if isProjectSetUp then logInfo else logLifecycle
    )
