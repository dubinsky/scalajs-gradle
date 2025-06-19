package org.podval.tools.build

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.InputFiles
import org.podval.tools.util.Files
import scala.jdk.CollectionConverters.SetHasAsScala
import java.io.File

abstract class LinkTask extends DefaultTask with BackendTask:
  @InputFiles def getRuntimeClassPath: ConfigurableFileCollection

  final def runtimeClassPath: Seq[File] = getRuntimeClassPath.getFiles.asScala.toSeq

  private val buildDirectory: File = getProject.getLayout.getBuildDirectory.get.getAsFile

  final protected def outputDirectory: File = Files.file(buildDirectory, "tmp", getName)

  final protected def outputFile(name: String): File = File(outputDirectory, name)

object LinkTask:
  abstract class Main extends LinkTask with BackendTask.Main

  abstract class Test extends LinkTask with BackendTask.Test
