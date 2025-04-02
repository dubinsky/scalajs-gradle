package org.podval.tools.scalajsplugin

import org.gradle.api.artifacts.Configuration
import org.gradle.api.{Project, Task}
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.tasks.{SourceSet, TaskProvider}
import org.podval.tools.build.{DependencyRequirement, Gradle, ScalaPlatform}
import scala.jdk.CollectionConverters.*

object BackendDelegate:
  final val sharedSourceRoot: String = "shared"

abstract class BackendDelegate(
  project: Project
):
  def sourceRoot: String

  def mainSourceSetName: String

  def testSourceSetName: String

  def configurationToAddToClassPath: Option[String]

  // TODO switch from Tasks to TaskProviders and move stuff from configureTask() to setUpProject()
  def setUpProject(): Unit

  def configureTask(task: Task): Unit

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

  protected final def getClassesTask(sourceSetName: String): Task = project
    .getTasks
    .getByName(Gradle.getSourceSet(project, sourceSetName).getClassesTaskName)

  protected final def getScalaCompile(sourceSetName: String): ScalaCompile = getClassesTask(sourceSetName)
    .getDependsOn
    .asScala
    .find(classOf[TaskProvider[ScalaCompile]].isInstance)
    .get
    .asInstanceOf[TaskProvider[ScalaCompile]]
    .get
