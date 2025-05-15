package org.podval.tools.scalajsplugin

import org.gradle.api.plugins.scala.ScalaBasePlugin
import org.gradle.api.tasks.SourceSet

// Names to use for various Gradle things: source sets, configurations, tasks...
object GradleNames:
  val sharedSourceRoot: String = "shared"
  
  def linkTaskName(sourceSet: SourceSet): String = sourceSet.getTaskName("link", "")
  def runTaskName (sourceSet: SourceSet): String = sourceSet.getTaskName("run" , "")
  
  def scalaCompilerPluginsConfigurationName(sourceSet: SourceSet): String =
    sourceSet.getTaskName(ScalaBasePlugin.SCALA_COMPILER_PLUGINS_CONFIGURATION_NAME, "")
