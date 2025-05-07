package org.podval.tools.scalajsplugin.scalajs

import org.gradle.api.Project
import org.podval.tools.build.{DependencyRequirement, ScalaBackendKind, ScalaDependency, ScalaPlatform, ScalaVersion, 
  Version}
import org.podval.tools.node.NodeExtension
import org.podval.tools.scalajsplugin.nonjvm.{NonJvm, NonJvmLinkMainTask, NonJvmLinkTask, NonJvmLinkTestTask, 
  NonJvmRunMainTask, NonJvmTestTask}
import org.podval.tools.scalajsplugin.BackendDelegate
import org.podval.tools.test.framework.JUnit4ScalaJS
import scala.jdk.CollectionConverters.SeqHasAsJava

object ScalaJS extends NonJvm:
  override def linkMainTaskClass: Class[? <: NonJvmLinkMainTask[?]] = classOf[ScalaJSLinkMainTask]
  override def linkTestTaskClass: Class[? <: NonJvmLinkTestTask[?]] = classOf[ScalaJSLinkTestTask]
  override def runMainTaskClass : Class[? <: NonJvmRunMainTask [?]] = classOf[ScalaJSRunMainTask ]
  override def testTaskClass    : Class[? <: NonJvmTestTask    [?]] = classOf[ScalaJSTestTask    ]

  override def backendKind: ScalaBackendKind.NonJvm = ScalaBackendKind.JS
  override def sourceRoot: String = "js"
  override def gradleNamesSuffix: String = "JS"
  override def pluginDependenciesConfigurationName: String = "scalajs"
  override def areCompilerPluginsBuiltIntoScala3: Boolean = true
  override def junit4: ScalaDependency.Maker = JUnit4ScalaJS
  override def scalaCompileParameters(isScala3: Boolean): Seq[String] = if !isScala3 then Seq.empty else Seq("-scalajs")
  override def versionExtractor(version: Version): Version = version
  override def versionComposer(projectScalaVersion: Version, backendVersion: Version): Version = backendVersion.simple
  
  override def createExtensions(project: Project): Unit =
    val nodeExtension: NodeExtension = NodeExtension.addTo(project)
    nodeExtension.getModules.convention(List("jsdom").asJava)
  
  private val group: String = "org.scala-js"
  
  private sealed class Maker(
    final override val artifact: String,
    what: String,
    final override val isScalaVersionFull: Boolean = false
  ) extends ScalaDependency.MakerScala2Jvm:
    final override def description: String = describe(what)
    final override def versionDefault: Version = ScalaJS.backendKind.versionDefault
    final override def group: String = ScalaJS.group

  override def implementation: Seq[ScalaDependency.Maker] = Seq.empty
  override def library(isScala3: Boolean): ScalaDependency.Maker = Maker("scalajs-library", "Library")
  override def linker: ScalaDependency.Maker = Maker("scalajs-linker", "Linker")
  override def testAdapter: ScalaDependency.Maker = Maker("scalajs-sbt-test-adapter", "Test Adapter for Node.js")
  //  object TestInterface extends Maker("scalajs-test-interface")
  override def testBridge: ScalaDependency.Maker = new Maker("scalajs-test-bridge", "Test Bridge for Node.js"):
    override def useExactVersionInVerifyRequired: Boolean = true

  // compiler plugins for Scala 2
  override def compiler: ScalaDependency.Maker = Maker(
    "scalajs-compiler",
    "Compiler Plugin for Scala 2",
    isScalaVersionFull = true
  )
  
  override def junit4Plugin: ScalaDependency.Maker = Maker(
    "scalajs-junit-test-plugin",
    "JUnit4 Compiler Plugin for generating bootstrappers for Scala 2",
    isScalaVersionFull = true
  )
  
  object JSDomNodeJS extends ScalaDependency.MakerScala2Jvm:
    override val versionDefault: Version = Version("1.1.0")
    override def group: String = ScalaJS.group
    override val artifact: String = "scalajs-env-jsdom-nodejs"
    override val description: String = describe("Library for DOM manipulations on Node.js")

  object DomSJS extends ScalaDependency.Maker:
    override val versionDefault: Version = Version("2.8.0")
    override def group: String = ScalaJS.group
    override val artifact: String = "scalajs-dom"
    override val description: String = describe("Library for DOM manipulations")

  override def additionalPluginDependencyRequirements: Seq[DependencyRequirement[ScalaPlatform]] = Seq(
    // ScalaModules.ParallelCollections,
    JSDomNodeJS.required()
  )

  override def additionalImplementationDependencyRequirements(
    backendVersion: Version,
    projectScalaPlatform: ScalaPlatform
  ): Seq[DependencyRequirement[ScalaPlatform]] = Seq(
    DomSJS.required()
  ) ++
    // only for Scala 3
    (if !projectScalaPlatform.version.isScala3 then Seq.empty else Seq(
      ScalaVersion.Scala3.ScalaLibraryJS.required(projectScalaPlatform.scalaVersion)
    ))
