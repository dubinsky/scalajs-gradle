package org.podval.tools.scalajsplugin

import org.gradle.api.plugins.scala.ScalaBasePlugin
import org.gradle.util.internal.GUtil

// Names to use for various Gradle things: source sets, configurations, tasks...
object GradleNames:
  val sharedSourceRoot: String = "shared"

final class GradleNames(suffix: String):
  def suffix(what: String): String = GUtil.toLowerCamelCase(s"$what $suffix")

  def scalaCompilerPluginsConfigurationName: String = suffix(ScalaBasePlugin.SCALA_COMPILER_PLUGINS_CONFIGURATION_NAME)

  def mainSourceSetName: String = if suffix.isEmpty then "main" else suffix // also feature name
  def linkTaskName: String = suffix("link")
  def runTaskName: String = suffix("run")

  def testSourceSetName: String = suffix("test") // also test suite name and test task name
  def testLinkTaskName: String = suffix("linkTest")
