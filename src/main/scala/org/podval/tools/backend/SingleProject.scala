package org.podval.tools.backend

import org.gradle.api.Project
import org.podval.tools.gradle.{Projects, ScalaExtension}

abstract class SingleProject(project: Project) extends BackendProject(project):
  final protected def setScalaVersionFromParent(parent: Project): Unit =
    Projects.afterEvaluateIfAvailable(parent, ScalaExtension
      .findScalaVersion(parent)
      .foreach(ScalaExtension.setScalaVersion(project, _))
    )
