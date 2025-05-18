package org.podval.tools.build

import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JvmTestSuitePlugin
import org.gradle.util.internal.GUtil
import org.podval.tools.build.jvm.JvmBackend
import org.podval.tools.build.scalajs.ScalaJSBackend
import org.podval.tools.build.scalanative.ScalaNativeBackend

trait ScalaBackend derives CanEqual:
  def name: String
  def displayName: String
  def sourceRoot: String
  def suffixOpt: Option[String]
  def testsCanNotBeForked: Boolean

  final def suffixString: String = suffixOpt.map(suffix => s"_$suffix").getOrElse("")

  // The name of the test suite, its source set, and the test task.
  final def testSuiteName: String =
    GUtil.toLowerCamelCase(s"${ScalaBackend.defaultTestSuiteName} $sourceRoot")

  final def testImplementationConfigurationName: String =
    GUtil.toLowerCamelCase(s"test $sourceRoot implementation")
  
  final protected def describe(what: String): String = s"$displayName $what."

  def scalaCompileParameters(isScala3: Boolean): Seq[String]

  def dependencyRequirements(
    implementationConfiguration: Configuration,
    testImplementationConfiguration: Configuration,
    projectScalaPlatform: ScalaPlatform
  ): BackendDependencyRequirements

object ScalaBackend:
  def all: Set[ScalaBackend] = Set(JvmBackend, ScalaJSBackend, ScalaNativeBackend)

  val sharedSourceRoot: String = "shared"

  def defaultTestSuiteName: String = JvmTestSuitePlugin.DEFAULT_TEST_SUITE_NAME

  def testSuiteName(backend: Option[ScalaBackend]): String = backend
    .map(_.testSuiteName)
    .getOrElse(defaultTestSuiteName)
