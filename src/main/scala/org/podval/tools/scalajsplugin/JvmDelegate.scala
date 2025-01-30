package org.podval.tools.scalajsplugin

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.podval.tools.build.{JavaDependency, ScalaLibrary}
import org.podval.tools.testing.Sbt

class JvmDelegate extends ScalaJSPlugin.Delegate:
  override def beforeEvaluate(project: Project): Unit =
    project.getTasks.replace("test", classOf[JvmTestTask])

  override def afterEvaluate(
    project: Project,
    pluginScalaLibrary : ScalaLibrary,
    projectScalaLibrary: ScalaLibrary
  ): Unit =
    JvmDelegate.SbtTestInterfaceDependencyRequirement.applyToConfiguration(project)

object JvmDelegate:
  private object SbtTestInterfaceDependencyRequirement extends JavaDependency.Requirement(
    dependency = Sbt.TestInterface,
    version = Sbt.TestInterface.versionDefault,
    reason =
      """because some test frameworks (ScalaTest :)) do not bring it in in,
        |and it needs to be on the testImplementation classpath
        |""".stripMargin,
    configurationName = JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME
  )
