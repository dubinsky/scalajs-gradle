package org.podval.tools.build

import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.scala.ScalaBasePlugin

final class Configurations(
  val configuration: String, // configuration to add the dependency to
  val classPath    : String  // same or derived configuration with the resulting classpath
)

object Configurations:
  def forName(name: String): Configurations = Configurations(
    configuration = name,
    classPath     = name
  )

  val implementationConfiguration: String = JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME
  val implementationClassPath: String = JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME

  val implementation: Configurations = Configurations(
    configuration = implementationConfiguration,
    classPath     = implementationClassPath
  )

  val testImplementation: Configurations = Configurations(
    configuration = JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME,
    classPath     = JavaPlugin.TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME
  )

  val scalaCompilerPlugins: Configurations =
    Configurations.forName(ScalaBasePlugin.SCALA_COMPILER_PLUGINS_CONFIGURATION_NAME)
