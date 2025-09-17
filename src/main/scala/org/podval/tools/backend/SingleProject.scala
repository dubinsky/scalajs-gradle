package org.podval.tools.backend

import org.gradle.api.Project
import org.podval.tools.build.ScalaVersion
import org.podval.tools.gradle.{ScalaExtension, Sources}

abstract class SingleProject(project: Project) extends BackendProject(project):
  final def setScalaVersion(scalaVersion: ScalaVersion): Unit =
    ScalaExtension.setScalaVersion(project, scalaVersion)

  final def addVersionSpecificSources(scalaVersion: ScalaVersion): Unit =
    project.afterEvaluate(Sources.addVersionSpecific(_, scalaVersion))
