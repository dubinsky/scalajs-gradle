package org.podval.tools.nonjvm

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.{InputFiles, Internal, TaskAction}
import org.podval.tools.build.BackendTask
import org.podval.tools.util.Files
import scala.jdk.CollectionConverters.SetHasAsScala
import java.io.File

abstract class LinkTask[B <: NonJvmBackend] extends DefaultTask with BackendTask[B]:
  def link: Link[B]

  @TaskAction final def execute(): Unit = link.link()
  
  @Internal def isTest: Boolean

  @InputFiles def getRuntimeClassPath: ConfigurableFileCollection
  final def runtimeClassPath: Seq[File] = getRuntimeClassPath.getFiles.asScala.toSeq

  private val buildDirectory: File = getProject.getLayout.getBuildDirectory.get.getAsFile
  final protected def outputDirectory: File = Files.file(buildDirectory, "tmp", getName)
  final protected def outputFile(name: String): File = File(outputDirectory, name)

object LinkTask:
  abstract class Main[B <: NonJvmBackend] extends LinkTask[B]:
    override def isTest: Boolean = false

  abstract class Test[B <: NonJvmBackend] extends LinkTask[B]:
    override def isTest: Boolean = true
