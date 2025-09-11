package org.podval.tools.backend

import org.gradle.api.Project
import org.podval.tools.gradle.Sources

abstract class BackendProject(project: Project) extends Logging(project):
  def apply(): Unit

  final def addVersionSpecificSources(): Unit = Sources.addVersionSpecific(project, getScalaVersionFromScalaExtension)
