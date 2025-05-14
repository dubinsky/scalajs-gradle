package org.podval.tools.scalajsplugin.nonjvm

import org.podval.tools.scalajsplugin.BackendTask
import org.podval.tools.util.Files
import java.io.File

trait NonJvmLinkTask[L <: NonJvmLinkTask[L]] extends NonJvmTask[L]
  with BackendTask.HasRuntimeClassPath
  with BackendTask.DependsOnClasses:
  
  this: L =>
  
  final override protected def linkTask: L = this

  // TODO depends on GradleNames etc.
  private val buildDirectory: File = getProject.getLayout.getBuildDirectory.get.getAsFile
  final protected def outputFile(name: String): File = Files.file(buildDirectory, buildSubDirectory, getName, name)

  protected def buildSubDirectory: String

object NonJvmLinkTask:
  abstract class Main[L <: NonJvmLinkTask[L]] extends BackendTask.Link.Main with NonJvmLinkTask[L]:
    this: L =>

  abstract class Test[L <: NonJvmLinkTask[L]] extends BackendTask.Link.Test with NonJvmLinkTask[L]:
    this: L =>
