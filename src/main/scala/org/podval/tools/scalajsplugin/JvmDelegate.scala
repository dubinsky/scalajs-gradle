package org.podval.tools.scalajsplugin

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.podval.tools.build.{DependencyRequirement, JavaDependency, ScalaLibrary}
import org.podval.tools.testing.Sbt

class JvmDelegate extends ScalaJSPlugin.Delegate:
  override def beforeEvaluate(project: Project): Unit =
    project.getTasks.replace("test", classOf[JvmTestTask])

  override def afterEvaluate(
    project: Project,
    pluginScalaLibrary : ScalaLibrary,
    projectScalaLibrary: ScalaLibrary
  ): Unit =
    JvmDelegate.sbtTestInterfaceDependencyRequirement.applyToConfiguration(project)

object JvmDelegate:
  private def sbtTestInterfaceDependencyRequirement: DependencyRequirement = Sbt.TestInterface.dependency.required(
    version = Sbt.TestInterface.versionDefault,
    reason =
      """because some test frameworks (ScalaTest :)) do not bring it in in,
        |and it needs to be on the testImplementation classpath
        |""".stripMargin,
    configurationName = JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME
  )
