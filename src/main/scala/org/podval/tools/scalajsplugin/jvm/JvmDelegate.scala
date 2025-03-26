package org.podval.tools.scalajsplugin.jvm

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.podval.tools.build.{DependencyRequirement, ScalaPlatform}
import org.podval.tools.scalajsplugin.BackendDelegate
import org.podval.tools.test.SbtTestInterface

final class JvmDelegate(project: Project) extends BackendDelegate(project):
  override def setUpProject(): Unit =
    project.getTasks.replace("test", classOf[JvmTestTask])

  override def configurationToAddToClassPath: Option[String] = None

  override def configureProject(projectScalaPlatform: ScalaPlatform): Unit = ()
  
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
