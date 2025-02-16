package org.podval.tools.scalajsplugin

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.podval.tools.build.{DependencyRequirement, ScalaPlatform}
import org.podval.tools.testing.Sbt

class JvmDelegate extends ScalaJSPlugin.Delegate:
  override def beforeEvaluate(project: Project): Unit =
    project.getTasks.replace("test", classOf[JvmTestTask])

  override def afterEvaluate(
    project: Project,
    pluginScalaPlatform: ScalaPlatform,
    projectScalaPlatform: ScalaPlatform
  ): Unit =
    val sbtTestInterfaceDependencyRequirement: DependencyRequirement = Sbt.TestInterface.required(
      platform = projectScalaPlatform,
      version = Sbt.TestInterface.versionDefault,
      reason =
        """because some test frameworks (ScalaTest :)) do not bring it in in,
          |and it needs to be on the testImplementation classpath
          |""".stripMargin,
      configurationName = JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME
    )
    
    sbtTestInterfaceDependencyRequirement.applyToConfiguration(project)
