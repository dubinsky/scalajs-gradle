package org.podval.tools.scalajsplugin

import org.gradle.api.Project
import org.podval.tools.build.{DependencyRequirement, ScalaPlatform}

abstract class BackendDelegate(protected val project: Project):
  def setUpProject(): Unit

  def configurationToAddToClassPath: Option[String]

  def configureProject(projectScalaPlatform: ScalaPlatform): Unit

  def dependencyRequirements(
    pluginScalaPlatform: ScalaPlatform,
    projectScalaPlatform: ScalaPlatform
  ): Seq[DependencyRequirement]

