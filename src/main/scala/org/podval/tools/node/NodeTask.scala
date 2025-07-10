package org.podval.tools.node

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.{Input, TaskAction}

abstract class NodeTask(commandName: String) extends DefaultTask with TaskWithNodeProject:
  setGroup("other")
  setDescription(s"Runs command supplied with the command line option '--$commandName-arguments' using '$commandName'.")

  // setArguments() can not be declared here, and is instead declared in subclasses,
  // since it is annotated - and annotation arguments must be constant...
  private var arguments: String = ""
  final protected def argumentsInternal(arguments: String): Unit = this.arguments = arguments
  @Input def getArguments: String = arguments
  final protected def argumentsList: List[String] = arguments.split(" ").toList

object NodeTask:
  abstract class NodeRunTask extends NodeTask("node"):
    @TaskAction final def execute(): Unit = nodeProject.node(argumentsList)

    @org.gradle.api.tasks.options.Option(
      option = "node-arguments",
      description = "The command to execute with 'node'."
    )
    def setArguments(arguments: String): Unit = argumentsInternal(arguments)

  abstract class NpmRunTask extends NodeTask("npm"):
    @TaskAction final def execute(): Unit = nodeProject.npm(argumentsList)

    @org.gradle.api.tasks.options.Option(
      option = "npm-arguments",
      description = "The command to execute with 'npm'."
    )
    def setArguments(arguments: String): Unit = argumentsInternal(arguments)
