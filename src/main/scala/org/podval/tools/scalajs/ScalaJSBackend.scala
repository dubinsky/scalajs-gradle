package org.podval.tools.scalajs

import org.podval.tools.build.{BackendTask, DependencyMaker, DependencyRequirement, PreVersion, ScalaDependencyMaker, 
  ScalaVersion, Version}
import org.podval.tools.jvm.JvmBackend
import org.podval.tools.node.NodeExtension
import org.podval.tools.nonjvm.NonJvmBackend
import org.podval.tools.test.framework.JUnit4ScalaJS

case object ScalaJSBackend extends NonJvmBackend(
  name = "Scala.js",
  sourceRoot = "js",
  artifactSuffix = "sjs1",
  pluginDependenciesConfigurationName = "scalajs",
  createExtension = Some(NodeExtension.create)
):
  override val versionDefault: Version = Version("1.19.0")

  override def linkTaskClass    : Class[ScalaJSLinkTask.Main] = classOf[ScalaJSLinkTask.Main]
  override def testLinkTaskClass: Class[ScalaJSLinkTask.Test] = classOf[ScalaJSLinkTask.Test]
  override def runTaskClass     : Class[ScalaJSRunTask .Main] = classOf[ScalaJSRunTask .Main]
  override def testTaskClass    : Class[ScalaJSRunTask .Test] = classOf[ScalaJSRunTask .Test]

  override def scalaCompileParameters(scalaVersion: ScalaVersion): Seq[String] =
    if scalaVersion.isScala3 
    then Seq("-scalajs")
    else Seq.empty
    
  override def areCompilerPluginsBuiltIntoScala3: Boolean = true
  override def junit4: DependencyMaker = JUnit4ScalaJS.forJS.get.maker
  override def versionExtractor(version: PreVersion): Version = version.simple
  
  override def versionComposer(
    projectScalaVersion: ScalaVersion,
    backendVersion: Version
  ): Version = backendVersion

  val group: String = "org.scala-js"

  private sealed class Maker(
    final override val artifact: String,
    what: String,
    final override val isScalaVersionFull: Boolean = false
  ) extends ScalaDependencyMaker:
    final override def description: String = describe(what)
    final override def versionDefault: Version = ScalaJSBackend.versionDefault
    final override def group: String = ScalaJSBackend.group
    final override def scalaBackend: JvmBackend.type = JvmBackend
    final override def isPublishedForScala3: Boolean = false

  override def implementation: Array[ScalaDependencyMaker] = Array.empty
  override def library(scalaVersion: ScalaVersion): ScalaDependencyMaker = Maker("scalajs-library", "Library")
  override def linker: ScalaDependencyMaker = Maker("scalajs-linker", "Linker")
  override def testAdapter: ScalaDependencyMaker = Maker("scalajs-sbt-test-adapter", "Test Adapter for Node.js")
  //  object TestInterface extends Maker("scalajs-test-interface")
  override def testBridge: ScalaDependencyMaker = new Maker("scalajs-test-bridge", "Test Bridge for Node.js"):
    override def useExactVersionInVerifyRequired: Boolean = true

  // compiler plugins for Scala 2
  override def compiler: ScalaDependencyMaker = Maker(
    "scalajs-compiler",
    "Compiler Plugin for Scala 2",
    isScalaVersionFull = true
  )

  override def junit4Plugin: ScalaDependencyMaker = Maker(
    "scalajs-junit-test-plugin",
    "JUnit4 Compiler Plugin for generating bootstrappers for Scala 2",
    isScalaVersionFull = true
  )
  
  override def additionalPluginDependencyRequirements: Array[DependencyRequirement] = Array(
    JSDomNodeJSEnv.required(),
    //PlaywrightJSEnv.required()
  )

  override def additionalImplementationDependencyRequirements(
    backendVersion: Version,
    scalaVersion: ScalaVersion
  ): Array[DependencyRequirement] =
    if !scalaVersion.isScala3 // ++ is from IterableOnce and thus is not available on Scala 2.12...
    then Array(DomSJS.required()) 
    else Array(DomSJS.required(), Scala3LibraryJS.required(scalaVersion.version)) // only for Scala 3
