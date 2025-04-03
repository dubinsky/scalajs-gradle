package org.podval.tools.scalajsplugin

import org.gradle.api.artifacts.Configuration
import org.gradle.api.{Project, Task}
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.tasks.{SourceSet, TaskProvider}
import org.podval.tools.build.{DependencyRequirement, Gradle, ScalaPlatform}
import scala.jdk.CollectionConverters.*

abstract class BackendDelegate(project: Project):
  def configurationToAddToClassPath: Option[String]

  def setUpProject(): TestTaskMaker[?]
  
  def configureProject(isScala3: Boolean): Unit

  def dependencyRequirements(
    pluginScalaPlatform: ScalaPlatform,
    projectScalaPlatform: ScalaPlatform
  ): Seq[DependencyRequirement]

  // -------------------------------------------------------------------------------------------
  // following code for setting up source sets, configurations and other Gradle things for Scala
  // was copied, translates and adjusted for jvm/js/shared split from the Gradle sources...
  // -------------------------------------------------------------------------------------------

  protected final def createConfiguration(name: String, description: String): Configuration =
    val result: Configuration = project.getConfigurations.create(name)
    result.setVisible(false)
    result.setCanBeConsumed(false)
    result.setDescription(description)
    result

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
