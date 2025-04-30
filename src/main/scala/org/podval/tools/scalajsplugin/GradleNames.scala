package org.podval.tools.scalajsplugin

import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.scala.ScalaBasePlugin
import org.gradle.api.tasks.SourceSet

// Names to use for various Gradle things: source sets, configurations, tasks...
final class GradleNames(suffix: String):
  private def s(string: String) = string + suffix
  val mainSourceSetName: String = s(SourceSet.MAIN_SOURCE_SET_NAME)
  val implementationConfigurationName: String = s(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)
  val testSourceSetName: String= s(SourceSet.TEST_SOURCE_SET_NAME)
  val testImplementationConfigurationName: String = s(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME)
  val scalaCompilerPluginsConfigurationName: String = s(ScalaBasePlugin.SCALA_COMPILER_PLUGINS_CONFIGURATION_NAME)
  val runtimeClasspathConfigurationName: String = s(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)
  val scalaCompileTaskName: String = s("scala")
  val testTaskName: String = s("test")
