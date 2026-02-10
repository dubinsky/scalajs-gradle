package org.podval.tools.node

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.{CacheableTask, Input, TaskAction}

@CacheableTask
abstract class NodeTask extends DefaultTask with NodeProjectTask:
  // setArguments() can not be declared here, and is instead declared in subclasses,
  // since it is annotated - and annotation arguments must be constant...
  private var arguments: String = ""
  final protected def argumentsInternal(arguments: String): Unit = this.arguments = arguments
  @Input def getArguments: String = arguments
  final protected def argumentsList: List[String] = arguments.split(" ").toList

object NodeTask:
  @CacheableTask
  abstract class NodeRunTask extends NodeTask:
    @TaskAction final def execute(): Unit = nodeProject.node(argumentsList, log = true)

    @org.gradle.api.tasks.options.Option(
      option = "node-arguments",
      description = "The command to execute with 'node'."
    )
    def setArguments(arguments: String): Unit = argumentsInternal(arguments)

  @CacheableTask
  abstract class NpmRunTask extends NodeTask:
    @TaskAction final def execute(): Unit = nodeProject.npm(argumentsList, log = true)

    @org.gradle.api.tasks.options.Option(
      option = "npm-arguments",
      description = "The command to execute with 'npm'."
    )
    def setArguments(arguments: String): Unit = argumentsInternal(arguments)
