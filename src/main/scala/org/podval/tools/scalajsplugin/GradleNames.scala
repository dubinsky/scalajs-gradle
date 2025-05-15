package org.podval.tools.scalajsplugin

import org.gradle.api.plugins.JvmTestSuitePlugin
import org.gradle.api.plugins.scala.ScalaBasePlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.util.internal.GUtil
import org.podval.tools.build.ScalaBackendKind

// Names to use for various Gradle things: source sets, configurations, tasks...
object GradleNames:
  val sharedSourceRoot: String = "shared"

  def testSuiteName(backend: ScalaBackendKind): String =
    GUtil.toLowerCamelCase(s"${JvmTestSuitePlugin.DEFAULT_TEST_SUITE_NAME} ${backend.sourceRoot}")
  
  // TODO this better return the same result as the source set :
  def testImplementationConfigurationName(backend: ScalaBackendKind): String =
    GUtil.toLowerCamelCase(s"test ${backend.sourceRoot} implementation")
    
  def linkTaskName(sourceSet: SourceSet): String = sourceSet.getTaskName("link", "")
  def runTaskName (sourceSet: SourceSet): String = sourceSet.getTaskName("run" , "")
  
  def scalaCompilerPluginsConfigurationName(sourceSet: SourceSet): String =
    sourceSet.getTaskName(ScalaBasePlugin.SCALA_COMPILER_PLUGINS_CONFIGURATION_NAME, "")
