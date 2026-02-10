package org.podval.tools.scalajs

import org.gradle.api.Project
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.podval.tools.build.{DependencyRequirement, ScalaBinaryVersion, ScalaDependency, ScalaLibrary, Version}
import org.podval.tools.node.NodeExtension
import org.podval.tools.nonjvm.NonJvmBackend

object ScalaJSBackend extends NonJvmBackend(
  name = "Scala.js",
  group = "org.scala-js",
  versionDefault = Version("1.20.2"),
  sourceRoot = "js",
  artifactSuffix = "sjs1",
  pluginDependenciesConfigurationName = "scalajs",
  areCompilerPluginsBuiltIntoScala3 = true
):
  override protected def linkTaskClass    : Class[ScalaJSLinkTask.Main] = classOf[ScalaJSLinkTask.Main]
  override protected def testLinkTaskClass: Class[ScalaJSLinkTask.Test] = classOf[ScalaJSLinkTask.Test]
  override protected def runTaskClass     : Class[ScalaJSRunTask .Main] = classOf[ScalaJSRunTask .Main]
  override protected def testTaskClass    : Class[ScalaJSRunTask .Test] = classOf[ScalaJSRunTask .Test]

  override protected def compilerPlugin: ScalaDependency =
    scalaDependency(artifact = "scalajs-compiler", what = "Compiler Plugin for Scala 2").scala2
  
  override protected def junit4Plugin: ScalaDependency =
    scalaDependency(artifact = "scalajs-junit-test-plugin", what = "JUnit4 Compiler Plugin for Scala 2").scala2
  
  override protected def linker: ScalaDependency =
    scalaDependency(artifact = "scalajs-linker", what = "Linker").scala2
  
  override protected def testAdapter: ScalaDependency =
    scalaDependency(artifact = "scalajs-sbt-test-adapter", what = "Test Adapter for Node.js").scala2
  
  override protected def testBridge: ScalaDependency =
    scalaDependency(artifact = "scalajs-test-bridge", what = "Test Bridge for Node.js").scala2.jvm

  override protected def library(scalaLibrary: ScalaLibrary): ScalaDependency =
    scalaDependency(artifact = "scalajs-library", what = "Library").scala2.jvm

  override protected def withBackendVersion: Array[ScalaDependency] = Array.empty

  val jsDomNode: ScalaDependency = scalaDependency(
    artifact = "scalajs-env-jsdom-nodejs",
    what = "Node.js JavaScript environment with JSDOM",
    versionDefault = Version("1.1.1")
  )
    .scala2

  override protected def pluginDependencies: Array[ScalaDependency] = Array(
    jsDomNode
  )

  val dom: ScalaDependency = scalaDependency(
    artifact = "scalajs-dom",
    what = "Library for DOM manipulations",
    versionDefault = Version("2.8.1")
  )

  override protected def withDefaultVersion: Array[ScalaDependency] = Array(
    dom
  )

  //def javaLogging: ScalaDependency =
  // scalaDependency(
  //   artifact = "scalajs-java-logging",
  //   what = "Port of the java.util.logging API of JDK 8 for Scala.js"),
  //   versionDefault = Version("1.0.0")
  // )
  //   .scala2
  
  val playwright: ScalaDependency = scalaDependency(
    group = "io.github.gmkumar2005",
    artifact = "scala-js-env-playwright",
    what = "Playwright JavaScript environment",
    versionDefault = Version("0.1.18")
  )
    .scala2
    .jvm

  override protected def junit4: JUnit4ScalaJS.type = JUnit4ScalaJS

  override protected def scalaCompileParameters(scalaLibrary: ScalaLibrary): Seq[String] =
    scalaLibrary.scalaVersion.binaryVersion match
      case _: ScalaBinaryVersion.Scala3 => Seq("-scalajs")
      case _ => Seq.empty

  override protected def implementation(scalaLibrary: ScalaLibrary): Array[DependencyRequirement] =
    scalaLibrary.scalaVersion.binaryVersion match
      case _: ScalaBinaryVersion.Scala3 => Array(scala3Library.require(scalaLibrary.scalaVersion.version))
      case _ => Array.empty

  // There is no Scala 2 equivalent.
  private def scala3Library: ScalaDependency = scalaDependency(
    group = ScalaBinaryVersion.group,
    artifact = "scala3-library",
    what = "Scala 3 library in Scala.js",
  )
    .scala3

  override def apply(
    project: Project, 
    jvmPluginServices: JvmPluginServices,
    isRunningInIntelliJ: Boolean
  ): Unit =
    super.apply(project, jvmPluginServices, isRunningInIntelliJ)

    NodeExtension.create(project)
