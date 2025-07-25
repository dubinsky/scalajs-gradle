package org.podval.tools.nonjvm

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.podval.tools.build.BackendTask
import org.podval.tools.gradle.{Projects, TaskWithSourceSet}
import org.podval.tools.util.Files
import java.io.File

abstract class LinkTask[B <: NonJvmBackend] extends DefaultTask with BackendTask[B] with TaskWithSourceSet:
  @TaskAction final def execute(): Unit = link.link()
  def link: Link[B]

  private val buildDirectory: File = Projects.buildDirectoryFile(getProject)
  final protected def outputDirectory: File = Files.file(buildDirectory, "tmp", getName)
  final protected def outputFile(name: String): File = File(outputDirectory, name)

object LinkTask:
  abstract class Main[B <: NonJvmBackend] extends LinkTask[B]:
    override def isTest: Boolean = false

  abstract class Test[B <: NonJvmBackend] extends LinkTask[B]:
    override def isTest: Boolean = true
