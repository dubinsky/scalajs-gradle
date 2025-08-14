package org.podval.tools.backend

import org.gradle.api.Project
import org.podval.tools.build.ScalaVersion
import org.podval.tools.gradle.{Projects, ScalaExtension, Sources}

abstract class SingleProject(project: Project) extends BackendProject(project):
  final protected def setScalaVersionFromParentAndAddVersionSpecificSources(): Unit =
    Projects.parent(project).foreach: (parent: Project) =>
      Projects.afterEvaluateIfAvailable(parent, ScalaExtension
        .findScalaVersion(parent)
        .foreach: (scalaVersion: ScalaVersion) =>
          ScalaExtension.setScalaVersion(project, scalaVersion)
          Sources.addVersionSpecific(project, scalaVersion)
      )
