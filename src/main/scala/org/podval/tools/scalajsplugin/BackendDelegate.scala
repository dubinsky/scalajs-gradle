package org.podval.tools.scalajsplugin

import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.api.{Project, Task}
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.tasks.{SourceSet, TaskProvider}
import org.podval.tools.build.{DependencyRequirement, Gradle, ScalaLibrary, ScalaPlatform}
import org.podval.tools.scalajsplugin.gradle.ScalaBasePlugin
import scala.jdk.CollectionConverters.*

abstract class BackendDelegate(
  project: Project,
  isModeMixed: Boolean
):
  protected def kind: BackendDelegateKind

  protected final val gradleNames: GradleNames = GradleNames(if isModeMixed then kind.gradleNamesSuffix else "")

  final def setUpProjectAndTestTask(jvmPluginServices: JvmPluginServices): Unit =
    if isModeMixed then ScalaBasePlugin(
      project = project,
      jvmPluginServices = jvmPluginServices,
      isCreate = kind.isCreateForMixedMode,
      sourceRoot = kind.sourceRoot,
      sharedSourceRoot = BackendDelegateKind.sharedSourceRoot,
      gradleNames = gradleNames
    ).apply()

    val addTestTask: AddTestTask[?] = setUpProject()
    addTestTask.addTestTask(isModeMixed, project)
  
  protected def setUpProject(): AddTestTask[?]

  protected def configurationToAddToClassPath: Option[String]
  
  final def afterEvaluate(
    pluginScalaPlatform: ScalaPlatform
  ): AddToClassPath =
    val projectScalaLibrary: ScalaLibrary = 
      ScalaLibrary.getFromConfiguration(project, gradleNames.implementationConfigurationName)
      
    val projectScalaPlatform: ScalaPlatform = projectScalaLibrary.toPlatform(kind.backendKind)
    
    dependencyRequirements(pluginScalaPlatform, projectScalaPlatform).foreach(_.applyToConfiguration(project))
    
    configureProject(projectScalaPlatform.version.isScala3)
    
    AddToClassPath(
      configurationToAddToClassPath,
      projectScalaLibrary,
      gradleNames.runtimeClasspathConfigurationName
    )
  
  protected def dependencyRequirements(
    pluginScalaPlatform: ScalaPlatform,
    projectScalaPlatform: ScalaPlatform
  ): Seq[DependencyRequirement]

  protected def configureProject(isScala3: Boolean): Unit

  protected final def getClassesTask(sourceSet: SourceSet): Task = project
    .getTasks
    .getByName(sourceSet.getClassesTaskName)

  protected final def getScalaCompile(sourceSetName: String): ScalaCompile = Gradle.getClassesTask(project, sourceSetName)
    .getDependsOn
    .asScala
    .find(classOf[TaskProvider[ScalaCompile]].isInstance)
    .get
    .asInstanceOf[TaskProvider[ScalaCompile]]
    .get
