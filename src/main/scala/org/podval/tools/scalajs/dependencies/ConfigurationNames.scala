package org.podval.tools.scalajs.dependencies

import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.scala.ScalaBasePlugin

final class ConfigurationNames(
  val toAdd: String, // configuration to add the dependency to
  val toCheck: String  // same or derived configuration with the resulting classpath
)

object ConfigurationNames:
  val implementation: ConfigurationNames = ConfigurationNames(
    JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME,
    JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME
  )

  val testImplementation: ConfigurationNames = ConfigurationNames(
    JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME,
    JavaPlugin.TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME
  )

  val scalaCompilerPlugins: ConfigurationNames = ConfigurationNames(
    toAdd = ScalaBasePlugin.SCALA_COMPILER_PLUGINS_CONFIGURATION_NAME,
    toCheck = ScalaBasePlugin.SCALA_COMPILER_PLUGINS_CONFIGURATION_NAME
  )
