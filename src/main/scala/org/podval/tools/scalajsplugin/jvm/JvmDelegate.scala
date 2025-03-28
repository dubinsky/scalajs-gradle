package org.podval.tools.scalajsplugin.jvm

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.podval.tools.build.{DependencyRequirement, Gradle, ScalaPlatform}
import org.podval.tools.scalajsplugin.BackendDelegate
import org.podval.tools.test.SbtTestInterface
import scala.jdk.CollectionConverters.IterableHasAsScala

final class JvmDelegate(
  project: Project,
  objectFactory: ObjectFactory,
  isMixed: Boolean,
) extends BackendDelegate(
  project,
  objectFactory
):
  override def sourceRoot: String = JvmDelegate.sourceRoot

  override def mainSourceSetName: String = SourceSet.MAIN_SOURCE_SET_NAME

  override def testSourceSetName: String = SourceSet.TEST_SOURCE_SET_NAME

  override def configurationToAddToClassPath: Option[String] = None

  override def setUpProject(): Unit =
    project.getTasks.replace("test", classOf[JvmTestTask])

    if isMixed then configureSourceSetDefaults(isCreate = false)
  
  override def dependencyRequirements(
    pluginScalaPlatform: ScalaPlatform,
    projectScalaPlatform: ScalaPlatform
  ): Seq[DependencyRequirement] = Seq(
    SbtTestInterface.required(
      platform = projectScalaPlatform,
      reason =
        """because some test frameworks (ScalaTest :)) do not bring it in in,
          |and it needs to be on the testImplementation classpath
          |""".stripMargin,
      configurationName = JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME
    )
  )

  override def configureProject(projectScalaPlatform: ScalaPlatform): Unit =
    project.getTasks.asScala.foreach:
      case testTask: JvmTestTask =>
        testTask.getDependsOn.add(getClassesTask(testSourceSetName))
      case _ =>

object JvmDelegate:
  final val sourceRoot: String = "jvm"
