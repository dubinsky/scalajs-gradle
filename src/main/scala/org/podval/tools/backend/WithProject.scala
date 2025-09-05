package org.podval.tools.backend

import org.gradle.api.Project
import org.podval.tools.gradle.Projects

abstract class WithProject(val project: Project):
  final def name: String = Projects.projectDirName(project)
  final def is(candidate: Project): Boolean = Projects.projectDirName(candidate) == name

object WithProject:
  def names[T <: WithProject](projects: Set[T]): String = projects.map(_.name).mkString(", ")