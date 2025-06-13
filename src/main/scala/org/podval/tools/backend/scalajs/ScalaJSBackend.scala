package org.podval.tools.backend.scalajs

import org.podval.tools.backend.jvm.JvmBackend
import org.podval.tools.backend.nonjvm.NonJvmBackend
import org.podval.tools.build.{DependencyMaker, DependencyRequirement, PreVersion, ScalaBinaryVersion,
  ScalaDependencyMaker, ScalaModules, ScalaVersion, Version}
import org.podval.tools.test.framework.JUnit4ScalaJS

case object ScalaJSBackend extends NonJvmBackend:
  override val name: String = "Scala.js"
  override val sourceRoot: String = "js"
  override val artifactSuffix: String = "sjs1"
  override val versionDefault: Version = Version("1.19.0")

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
  ): PreVersion = backendVersion

  private val group: String = "org.scala-js"

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

  object JSDomNodeJSEnv extends ScalaDependencyMaker:
    override val versionDefault: Version = Version("1.1.0")
    override def group: String = ScalaJSBackend.group
    override val artifact: String = "scalajs-env-jsdom-nodejs"
    override val description: String = describe("Node.js JavaScript environment with JSDOM")
    override def scalaBackend: JvmBackend.type = JvmBackend
    override def isPublishedForScala3: Boolean = false

  object PlaywrightJSEnv extends ScalaDependencyMaker:
    override def group: String = "io.github.gmkumar2005"
    override def artifact: String = "scala-js-env-playwright"
    override def versionDefault: Version = Version("0.1.18")
    override val description: String = describe("Playwright JavaScript environment")
    override def scalaBackend: JvmBackend.type = JvmBackend
    override def isPublishedForScala3: Boolean = false
    override def isPublishedForScala213: Boolean = false

  object DomSJS extends ScalaDependencyMaker:
    override val versionDefault: Version = Version("2.8.0")
    override def group: String = ScalaJSBackend.group
    override val artifact: String = "scalajs-dom"
    override val description: String = describe("Library for DOM manipulations")
    override def scalaBackend: ScalaJSBackend.type = ScalaJSBackend

  override def additionalPluginDependencyRequirements: Array[DependencyRequirement] = Array(
    ScalaModules.ParallelCollections.required(),
    JSDomNodeJSEnv.required(),
//    PlaywrightJSEnv.required()
  )

  override def additionalImplementationDependencyRequirements(
    backendVersion: PreVersion,
    scalaVersion: ScalaVersion
  ): Array[DependencyRequirement] =
    if !scalaVersion.isScala3 // ++ is from IterableOnce and thus is not available on Scala 2.12...
    then Array(DomSJS.required()) 
    else Array(DomSJS.required(), ScalaBinaryVersion.Scala3.ScalaLibraryJS.required(scalaVersion.version)) // only for Scala 3
