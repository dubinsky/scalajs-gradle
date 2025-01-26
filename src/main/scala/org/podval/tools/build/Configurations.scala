package org.podval.tools.build

import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.scala.ScalaBasePlugin

object Configurations:
  val implementation: String = JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME
  val runtimeClassPath: String = JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME
  val testImplementation: String = JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME
  val zinc: String = ScalaBasePlugin.ZINC_CONFIGURATION_NAME
  val scalaCompilerPlugins: String = ScalaBasePlugin.SCALA_COMPILER_PLUGINS_CONFIGURATION_NAME
