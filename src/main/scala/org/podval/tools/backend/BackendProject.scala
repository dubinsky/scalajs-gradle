package org.podval.tools.backend

import org.gradle.api.Project

abstract class BackendProject(project: Project) extends WithProject(project):
  def announcement: String
  
  def apply(): Unit
  
  def afterEvaluate(): Unit
