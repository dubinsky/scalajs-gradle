package org.podval.tools.scalajs

import org.gradle.api.Project
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.podval.tools.build.{DependencyRequirement, PreVersion, ScalaDependencyMaker, ScalaVersion, Version}
import org.podval.tools.node.NodeExtension
import org.podval.tools.nonjvm.NonJvmBackend
import org.podval.tools.test.framework.JUnit4ScalaJS

object ScalaJSBackend extends NonJvmBackend(
  name = "Scala.js",
  group = "org.scala-js",
  versionDefault = Version("1.19.0"),
  sourceRoot = "js",
  artifactSuffix = "sjs1",
  pluginDependenciesConfigurationName = "scalajs",
  areCompilerPluginsBuiltIntoScala3 = true
):
  override protected def linkTaskClass    : Class[ScalaJSLinkTask.Main] = classOf[ScalaJSLinkTask.Main]
  override protected def testLinkTaskClass: Class[ScalaJSLinkTask.Test] = classOf[ScalaJSLinkTask.Test]
  override protected def runTaskClass     : Class[ScalaJSRunTask .Main] = classOf[ScalaJSRunTask .Main]
  override protected def testTaskClass    : Class[ScalaJSRunTask .Test] = classOf[ScalaJSRunTask .Test]

  override protected def scalaCompileParameters(scalaVersion: ScalaVersion): Seq[String] =
    if scalaVersion.isScala3 
    then Seq("-scalajs")
    else Seq.empty
    
  override protected def versionExtractor(version: PreVersion): Version = version.simple
  
  override protected def versionComposer(
    projectScalaVersion: ScalaVersion,
    backendVersion: Version
  ): Version = backendVersion

  override protected def junit4: JUnit4ScalaJS.type = JUnit4ScalaJS

  sealed class JvmScala2(artifact: String, what: String)
    extends Jvm(artifact, what) with ScalaDependencyMaker.Scala2

  override protected def library(isScala3: Boolean): JvmScala2 = JvmScala2("scalajs-library", "Library")
  override protected def compilerPlugin: Plugin = new JvmScala2("scalajs-compiler", "Compiler Plugin for Scala 2") with Plugin
  override protected def junit4Plugin: Plugin = new JvmScala2("scalajs-junit-test-plugin", "JUnit4 Compiler Plugin for generating bootstrappers for Scala 2") with Plugin
  override protected def linker: JvmScala2 = JvmScala2("scalajs-linker", "Linker")
  override protected def testAdapter: JvmScala2 = JvmScala2("scalajs-sbt-test-adapter", "Test Adapter for Node.js")
  override protected def testBridge: JvmScala2 = new JvmScala2("scalajs-test-bridge", "Test Bridge for Node.js"):
    override def isDependencyRequirementVersionExact: Boolean = true

  // object TestInterface extends Jvm("scalajs-test-interface")

  object JSDomNodeJSEnv extends JvmScala2("scalajs-env-jsdom-nodejs", "Node.js JavaScript environment with JSDOM"):
    override val versionDefault: Version = Version("1.1.0")

  override protected def pluginDependencies: Array[Jvm] = Array(JSDomNodeJSEnv)

  override protected def implementation: Array[BackendDependency] = Array.empty

  object DomSJS extends BackendDependency("scalajs-dom", "Library for DOM manipulations"):
    override def scalaBackend: ScalaJSBackend.type = ScalaJSBackend
    override val versionDefault: Version = Version("2.8.1")

  override protected def implementationDependencyRequirements(scalaVersion: ScalaVersion): Array[DependencyRequirement] =
    if !scalaVersion.isScala3 // ++ is from IterableOnce and thus is not available on Scala 2.12...
    then Array(DomSJS.required())
    else Array(DomSJS.required(), Scala3LibraryJS.required(scalaVersion.version))

  override def apply(
    project: Project, 
    jvmPluginServices: JvmPluginServices,
    isRunningInIntelliJ: Boolean
  ): Unit =
    super.apply(project, jvmPluginServices, isRunningInIntelliJ)

    project.getExtensions.create("node", classOf[NodeExtension])
