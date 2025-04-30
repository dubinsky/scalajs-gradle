package org.podval.tools.scalajsplugin.nonjvm

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.{InputFiles, Internal}
import org.podval.tools.util.Files
import java.io.File
import scala.jdk.CollectionConverters.SetHasAsScala

trait BackendLinkTask[L <: BackendLinkTask[L]] extends BackendTask[L]:
  this: L =>

  setGroup("build")

  @Internal protected def isTest: Boolean

  @InputFiles def getRuntimeClassPath: ConfigurableFileCollection
  final protected def runtimeClassPath: Seq[File] = getRuntimeClassPath.getFiles.asScala.toSeq

  final override protected def linkTask: L = this

  // TODO depends on GradleNames etc.
  private val buildDirectory: File = getProject.getLayout.getBuildDirectory.get.getAsFile
  final protected def outputFile(name: String): File = Files.file(buildDirectory, buildSubDirectory, getName, name)

  protected def buildSubDirectory: String
