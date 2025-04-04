package org.podval.tools.scalajsplugin

import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.scala.ScalaBasePlugin
import org.gradle.api.tasks.SourceSet

// Names to use for various Gradle things: source sets, configurations, tasks...
final class GradleNames(
  val mainSourceSetName: String,
  val implementationConfigurationName: String,
  val testSourceSetName: String,
  val testImplementationConfigurationName: String,
  val compilerPluginsConfigurationName: String,
  val runtimeClasspathConfigurationName: String,
  val scalaCompileTaskName: String
)

object GradleNames:
  // Names to use when running either JVM or Scala.js using one set of source sets: main and test.
  val jvm: GradleNames = GradleNames(
    mainSourceSetName = SourceSet.MAIN_SOURCE_SET_NAME,
    implementationConfigurationName = JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME,
    testSourceSetName = SourceSet.TEST_SOURCE_SET_NAME,
    testImplementationConfigurationName = JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME,
    compilerPluginsConfigurationName = ScalaBasePlugin.SCALA_COMPILER_PLUGINS_CONFIGURATION_NAME,
    runtimeClasspathConfigurationName = JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME,
    scalaCompileTaskName = "scala"
  )

  // Names to use when running both JVM and Scala.js using two sets of source sets: main and test and a new one.
  val js: GradleNames = GradleNames(
    mainSourceSetName = "mainJS",
    implementationConfigurationName = "implementationJS",
    testSourceSetName = "testJS",
    testImplementationConfigurationName = "testImplementationJS",
    compilerPluginsConfigurationName = "scalaJSCompilerPlugins",
    runtimeClasspathConfigurationName = "runtimeClasspathJS",
    scalaCompileTaskName = "scalaJS"
  )
