package org.podval.tools.backend

import org.gradle.api.Project

abstract class SingleProject(project: Project) extends BackendProject(project)

object SingleProject:
  def forEach[T <: SingleProject](projects: Set[T], f: Project => Unit): Unit =
    projects.map(_.project).foreach(f)
