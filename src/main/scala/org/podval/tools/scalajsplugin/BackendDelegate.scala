package org.podval.tools.scalajsplugin

import org.gradle.api.{Project, Task}
import org.gradle.api.tasks.TaskProvider
import org.podval.tools.build.{DependencyRequirement, ScalaBackendKind, ScalaPlatform}
import org.podval.tools.test.task.TestTask

trait BackendDelegate:
  def testTaskClass: Class[? <: TestTask]

  final def bind(isModeMixed: Boolean) = BackendDelegateBinding(
    this,
    GradleNames(if isModeMixed then sourceRoot else "")
  )

  final protected def describe(what: String): String = s"${backendKind.displayName} $what."

  def backendKind: ScalaBackendKind
  def sourceRoot: String
  def pluginDependenciesConfigurationNameOpt: Option[String]
  def scalaCompileParameters(isScala3: Boolean): Seq[String]

  def createExtensions(
    project: Project
  ): Unit
  
  // Returns an optional TaskProvider the test task depends on.
  def addTasks(
    project: Project, 
    gradleNames: GradleNames
  ): Option[TaskProvider[? <: Task]]

  def applyDependencyRequirements(
    project: Project,
    gradleNames: GradleNames,
    pluginScalaPlatform: ScalaPlatform,
    projectScalaPlatform: ScalaPlatform,
    isScala3: Boolean
  ): Unit

object BackendDelegate:
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

