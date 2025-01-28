package org.podval.tools.scalajs.jvm

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.podval.tools.build.{JavaDependency, ScalaLibrary, Version}
import org.podval.tools.scalajs.ScalaJSPlugin

class JvmDelegate extends ScalaJSPlugin.Delegate:
  override def beforeEvaluate(project: Project): Unit =
    project.getTasks.replace("test", classOf[JvmTestTask])

  override def afterEvaluate(
    project: Project,
    pluginScalaLibrary : ScalaLibrary,
    projectScalaLibrary: ScalaLibrary
  ): Unit =
    JavaDependency.Requirement(
      dependency = JavaDependency(group = "org.scala-sbt", artifact = "test-interface"),
      version = Version("1.0"),
      reason =
        """because some test frameworks (ScalaTest :)) do not bring it in in,
          |and it needs to be on the testImplementation classpath
          |""".stripMargin,
      configurationName = JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME
    ).applyToConfiguration(project)
