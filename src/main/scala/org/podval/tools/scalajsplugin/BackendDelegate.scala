package org.podval.tools.scalajsplugin

import org.gradle.api.Project
import org.podval.tools.build.{DependencyRequirement, ScalaPlatform}

abstract class BackendDelegate:
  def beforeEvaluate(project: Project): Unit

  def configurationToAddToClassPath: Option[String]

  def configureProject(
    project: Project,
    projectScalaPlatform: ScalaPlatform
  ): Unit

  def dependencyRequirements(
    project: Project,
    pluginScalaPlatform: ScalaPlatform,
    projectScalaPlatform: ScalaPlatform
  ): Seq[DependencyRequirement]
