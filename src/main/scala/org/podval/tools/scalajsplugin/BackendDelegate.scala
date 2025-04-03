package org.podval.tools.scalajsplugin

import org.gradle.api.{Project, Task}
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.tasks.{SourceSet, TaskProvider}
import org.podval.tools.build.{DependencyRequirement, Gradle, ScalaBackend, ScalaLibrary, ScalaPlatform}
import scala.jdk.CollectionConverters.*

abstract class BackendDelegate(
  project: Project,
  gradleNames: GradleNames
):
  def setUpProject(): TestTaskMaker[?]

  protected def backend: ScalaBackend

  protected def configurationToAddToClassPath: Option[String]
  
  def afterEvaluate(
    pluginScalaPlatform: ScalaPlatform
  ): AddToClassPath =
    val projectScalaLibrary: ScalaLibrary = 
      ScalaLibrary.getFromConfiguration(project, gradleNames.implementationConfigurationName)
      
    val projectScalaPlatform: ScalaPlatform = projectScalaLibrary.toPlatform(backend)
    
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
