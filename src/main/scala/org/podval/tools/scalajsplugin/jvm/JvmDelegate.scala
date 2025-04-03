package org.podval.tools.scalajsplugin.jvm

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.podval.tools.build.{DependencyRequirement, ScalaPlatform}
import org.podval.tools.scalajsplugin.{BackendDelegate, TestTaskMaker}
import org.podval.tools.test.SbtTestInterface
import scala.jdk.CollectionConverters.IterableHasAsScala

final class JvmDelegate(
  project: Project,
  mainSourceSetName: String,
  testSourceSetName: String
) extends BackendDelegate(
  project
):
  override def configurationToAddToClassPath: Option[String] = None

  override def configureProject(isScala3: Boolean): Unit = ()

  override def setUpProject(): TestTaskMaker[JvmTestTask] = TestTaskMaker[JvmTestTask](
    testSourceSetName,
    classOf[JvmTestTask],
    (_: JvmTestTask) => ()
  )

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
