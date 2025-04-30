package org.podval.tools.scalajsplugin

import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.api.Project
import org.podval.tools.build.{DependencyRequirement, ScalaLibrary, ScalaPlatform}
import org.podval.tools.scalajsplugin.gradle.ScalaBasePlugin
import org.slf4j.{Logger, LoggerFactory}

abstract class BackendDelegate(
  project: Project,
  isModeMixed: Boolean
):
  type DependencyRequirements = Seq[DependencyRequirement[ScalaPlatform]]

  protected def kind: BackendDelegateKind
  protected def gradleNamesSuffix: String
  protected def isCreateForMixedMode: Boolean
  protected def createConfigurations(): Unit
  protected def setUpProject(): AddTestTask[?]

  protected def afterEvaluate(
    pluginScalaPlatform: ScalaPlatform,
    projectScalaLibrary: ScalaLibrary,
    projectScalaPlatform: ScalaPlatform
  ): Option[AddToClassPath]

  final protected val gradleNames: GradleNames = GradleNames(if isModeMixed then gradleNamesSuffix else "")

  // Source sets, configurations, extensions, and tasks!
  final def apply(jvmPluginServices: JvmPluginServices): Unit =
    if isModeMixed then ScalaBasePlugin(
      project = project,
      jvmPluginServices = jvmPluginServices,
      isCreate = isCreateForMixedMode,
      sourceRoot = kind.sourceRoot,
      sharedSourceRoot = BackendDelegateKind.sharedSourceRoot,
      gradleNames = gradleNames
    ).apply()

    createConfigurations()

    val addTestTask: AddTestTask[?] = setUpProject()

    addTestTask.addTestTask(
      isModeMixed,
      project,
      gradleNames.testSourceSetName,
      gradleNames.testTaskName
    )

  final def afterEvaluate(pluginScalaPlatform: ScalaPlatform): Option[AddToClassPath] =
    val projectScalaLibrary: ScalaLibrary = ScalaLibrary.getFromConfiguration(
      project,
      gradleNames.implementationConfigurationName
    )
    val projectScalaPlatform: ScalaPlatform = projectScalaLibrary.toPlatform(kind.backendKind)

    afterEvaluate(
      pluginScalaPlatform,
      projectScalaLibrary,
      projectScalaPlatform
    )

  final protected def applyDependencyRequirements(
    dependencyRequirements: DependencyRequirements,
    scalaPlatform: ScalaPlatform,
    configurationName: String
  ): Unit = dependencyRequirements.map(_.applyToConfiguration(
    project,
    configurationName,
    scalaPlatform
  ))

object BackendDelegate:
  val logger: Logger = LoggerFactory.getLogger(BackendDelegate.getClass)
