package org.podval.tools.backend

import org.gradle.api.Project

abstract class BackendProject(val project: Project) extends Logging(project):
  def apply(): Unit
