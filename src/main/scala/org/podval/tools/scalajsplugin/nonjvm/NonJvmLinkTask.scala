package org.podval.tools.scalajsplugin.nonjvm

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.{InputFiles, Internal}
import org.podval.tools.util.Files
import scala.jdk.CollectionConverters.SetHasAsScala
import java.io.File

abstract class NonJvmLinkTask[L <: NonJvmLinkTask[L]] extends DefaultTask with NonJvmTask[L]:
  this: L =>
  
  @InputFiles def getRuntimeClassPath: ConfigurableFileCollection
  final protected def runtimeClassPath: Seq[File] = getRuntimeClassPath.getFiles.asScala.toSeq

  @Internal protected def isTest: Boolean
  
  final override protected def linkTask: L = this

  // TODO depends on GradleNames etc.
  private val buildDirectory: File = getProject.getLayout.getBuildDirectory.get.getAsFile
  final protected def outputFile(name: String): File = Files.file(buildDirectory, buildSubDirectory, getName, name)

  protected def buildSubDirectory: String
