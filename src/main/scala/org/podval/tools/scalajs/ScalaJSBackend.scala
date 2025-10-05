package org.podval.tools.scalajs

import org.gradle.api.Project
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.podval.tools.build.{DependencyRequirement, ScalaBinaryVersion, ScalaDependency, ScalaLibrary, Version}
import org.podval.tools.node.NodeExtension
import org.podval.tools.nonjvm.NonJvmBackend
import org.podval.tools.test.framework.JUnit4ScalaJS

object ScalaJSBackend extends NonJvmBackend(
  name = "Scala.js",
  group = "org.scala-js",
  versionDefault = Version("1.20.1"),
  sourceRoot = "js",
  artifactSuffix = "sjs1",
  pluginDependenciesConfigurationName = "scalajs",
  areCompilerPluginsBuiltIntoScala3 = true
):
  override def isJs    : Boolean = true
  override def isNative: Boolean = false

  override protected def linkTaskClass    : Class[ScalaJSLinkTask.Main] = classOf[ScalaJSLinkTask.Main]
  override protected def testLinkTaskClass: Class[ScalaJSLinkTask.Test] = classOf[ScalaJSLinkTask.Test]
  override protected def runTaskClass     : Class[ScalaJSRunTask .Main] = classOf[ScalaJSRunTask .Main]
  override protected def testTaskClass    : Class[ScalaJSRunTask .Test] = classOf[ScalaJSRunTask .Test]

  override protected def compilerPlugin: ScalaDependency =
    scalaDependency("scalajs-compiler", "Compiler Plugin for Scala 2").scala2
  override protected def junit4Plugin: ScalaDependency =
    scalaDependency("scalajs-junit-test-plugin", "JUnit4 Compiler Plugin for Scala 2").scala2
  override protected def linker: ScalaDependency =
    scalaDependency("scalajs-linker", "Linker").scala2
  override protected def testAdapter: ScalaDependency =
    scalaDependency("scalajs-sbt-test-adapter", "Test Adapter for Node.js").scala2
  override protected def testBridge: ScalaDependency =
    scalaDependency("scalajs-test-bridge", "Test Bridge for Node.js").scala2.jvm

  override protected def library(isScala3: Boolean): ScalaDependency =
    scalaDependency("scalajs-library", "Library").scala2.jvm

  override protected def withBackendVersion: Array[ScalaDependency] = Array.empty

  val jsDomNodeVersion: Version = Version("1.1.0")
  override protected def pluginDependencies: Array[ScalaDependency] = Array(
    scalaDependency("scalajs-env-jsdom-nodejs", "Node.js JavaScript environment with JSDOM")
      .withVersionDefault(jsDomNodeVersion)
      .scala2
  )
  
  val domVersion: Version = Version("2.8.1")
  override protected def withDefaultVersion: Array[ScalaDependency] = Array(
    scalaDependency("scalajs-dom", "Library for DOM manipulations")
      .withVersionDefault(domVersion)
  )

  //def javaLogging: ScalaDependency = scalaDependency("scalajs-java-logging", "Port of the java.util.logging API of JDK 8 for Scala.js")
  //  .withVersionDefault(Version("1.0.0")
  //  .scala2
  
  val playwrightVersion: Version = Version("0.1.18")
  //def playwright: ScalaDependency = scalaDependency("scala-js-env-playwright", "Playwright JavaScript environment")
  //  .withGroup("io.github.gmkumar2005")
  //  .withVersionDefault(playwrightVersion)
  //  .scala2
  //  .jvm

  override protected def junit4: JUnit4ScalaJS.type = JUnit4ScalaJS

  override protected def scalaCompileParameters(scalaLibrary: ScalaLibrary): Seq[String] =
    if scalaLibrary.isScala3
    then Seq("-scalajs")
    else Seq.empty

  override protected def implementation(scalaLibrary: ScalaLibrary): Array[DependencyRequirement] =
    if scalaLibrary.isScala3
    then Array(scala3Library.required(scalaLibrary.scalaVersion.version))
    else Array.empty

  // There is no Scala 2 equivalent.
  private def scala3Library: ScalaDependency = scalaDependency("scala3-library", "Scala 3 library in Scala.js")
    .withGroup(ScalaBinaryVersion.group)
    .withVersionDefault(ScalaBinaryVersion.Scala3.versionDefault)
    .scala3
  
  override def apply(
    project: Project, 
    jvmPluginServices: JvmPluginServices,
    isRunningInIntelliJ: Boolean
  ): Unit =
    super.apply(project, jvmPluginServices, isRunningInIntelliJ)

    NodeExtension.create(project)
