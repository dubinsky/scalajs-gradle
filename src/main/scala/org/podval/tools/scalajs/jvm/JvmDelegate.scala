package org.podval.tools.scalajs.jvm

import org.gradle.api.Project
import org.podval.tools.build.ScalaLibrary
import org.podval.tools.scalajs.ScalaJSPlugin

class JvmDelegate extends ScalaJSPlugin.Delegate:
  override def beforeEvaluate(project: Project): Unit =
    project.getTasks.replace("test", classOf[JvmTestTask])

  override def afterEvaluate(
    project: Project,
    pluginScalaLibrary : ScalaLibrary,
    projectScalaLibrary: ScalaLibrary
  ): Unit =
    JvmDependencies.SbtTestInterfaceDependencyRequirement.applyToConfiguration(project)
