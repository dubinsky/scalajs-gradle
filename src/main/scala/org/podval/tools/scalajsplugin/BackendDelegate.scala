package org.podval.tools.scalajsplugin

import org.gradle.api.Project
import org.podval.tools.build.{DependencyRequirement, ScalaBackendKind, ScalaPlatform}

trait BackendDelegate:
  final def bind(isModeMixed: Boolean) = BackendDelegateBinding(
    this,
    GradleNames(if isModeMixed then gradleNamesSuffix else "")
  )

  final protected def describe(what: String): String = s"${backendKind.displayName} $what."

  def backendKind: ScalaBackendKind
  def isCreateForMixedMode: Boolean
  def sourceRoot: String
  def gradleNamesSuffix: String
  def pluginDependenciesConfigurationNameOpt: Option[String]
  def scalaCompileParameters(isScala3: Boolean): Seq[String]

  def createExtensions(
    project: Project
  ): Unit
  
  def addTasks(
    project: Project, 
    gradleNames: GradleNames
  ): AddTestTask[?]

  def applyDependencyRequirements(
    project: Project,
    gradleNames: GradleNames,
    pluginScalaPlatform: ScalaPlatform,
    projectScalaPlatform: ScalaPlatform,
    isScala3: Boolean
  ): Unit

object BackendDelegate:
  val sharedSourceRoot: String = "shared"

  def applyDependencyRequirements(
    project: Project,
    dependencyRequirements: Seq[DependencyRequirement[ScalaPlatform]],
    scalaPlatform: ScalaPlatform,
    configurationName: String
  ): Unit = dependencyRequirements.map(_.applyToConfiguration(
    project,
    configurationName,
    scalaPlatform
  ))

