package org.podval.tools.build

import org.gradle.api.plugins.JvmTestSuitePlugin
import org.gradle.util.internal.GUtil

sealed trait ScalaBackendKind derives CanEqual:
  def name: String
  def displayName: String
  def sourceRoot: String
  def suffixOpt: Option[String]
  def testsCanNotBeForked: Boolean
  
  final def suffixString: String = suffixOpt.map(suffix => s"_$suffix").getOrElse("")

  // The name of the test suite, its source set, and the test task.
  final def testSuiteName: String =
    GUtil.toLowerCamelCase(s"${ScalaBackendKind.defaultTestSuiteName} $sourceRoot")

  def testImplementationConfigurationName: String =
    GUtil.toLowerCamelCase(s"test $sourceRoot implementation")

object ScalaBackendKind:
  val sharedSourceRoot: String = "shared"

  def defaultTestSuiteName: String = JvmTestSuitePlugin.DEFAULT_TEST_SUITE_NAME

  def testSuiteName(backend: Option[ScalaBackendKind]): String = backend
    .map(_.testSuiteName)
    .getOrElse(defaultTestSuiteName)

  def all: Set[ScalaBackendKind] = Set(JVM, JS, Native)
  
  case object JVM extends ScalaBackendKind:
    override val name: String = "JVM"
    override val displayName: String = "JVM"
    override val sourceRoot: String = "jvm"
    override val suffixOpt: Option[String] = None
    override val testsCanNotBeForked: Boolean = false

  sealed trait NonJvm extends ScalaBackendKind:
    final override def testsCanNotBeForked: Boolean = true
    final override def suffixOpt: Option[String] = Some(suffix)
    def suffix: String
    def versionDefault: Version
    
  case object JS extends NonJvm:
    override val name: String = "JS"
    override val displayName: String = "Scala.js"
    override val sourceRoot: String = "js"
    override val suffix: String = "sjs1"
    override val versionDefault: Version = Version("1.19.0")

  case object Native extends NonJvm:
    override val name: String = "Native"
    override val displayName: String = "Scala Native"
    override val sourceRoot: String = "native"
    override val suffix: String = "native0.5"
    override val versionDefault: Version = Version("0.5.7")
