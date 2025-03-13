package org.podval.tools.node

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.{Input, TaskAction}

abstract class NodeTask(commandName: String) extends DefaultTask with TaskWithNode:
  setGroup("other")
  setDescription(s"Run commands with '$commandName'")

  protected var arguments: String = ""
  @Input def getArguments: String = arguments

  // setArguments() can not be declared here, and is instead declared in subclasses,
  // since it is annotated - and annotation arguments must be constant...

  @TaskAction final def execute(): Unit = command(node)(arguments, getLogger.lifecycle(_))
  
  protected def command(node: Node): (String, String => Unit) => Unit

object NodeTask:
  abstract class NodeRunTask extends NodeTask("node"):
    override protected def command(node: Node): (String, String => Unit) => Unit = node.node

    @org.gradle.api.tasks.options.Option(
      option = "node-arguments",
      description = "The command to execute with 'node'"
    )
    def setArguments(value: String): Unit = arguments = value

  abstract class NpmRunTask extends NodeTask("npm"):
    override protected def command(node: Node): (String, String => Unit) => Unit = node.npm

    @org.gradle.api.tasks.options.Option(
      option = "npm-arguments",
      description = "The command to execute with 'npm'"
    )
    def setArguments(value: String): Unit = arguments = value
