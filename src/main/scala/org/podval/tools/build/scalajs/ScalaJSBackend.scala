package org.podval.tools.build.scalajs

import org.podval.tools.build.{DependencyRequirement, ScalaDependency, ScalaPlatform, ScalaVersion, Version}
import org.podval.tools.build.nonjvm.NonJvmBackend
import org.podval.tools.test.framework.JUnit4ScalaJS

case object ScalaJSBackend extends NonJvmBackend:
  override val name: String = "Scala.js"
  override val sourceRoot: String = "js"
  override val artifactSuffix: String = "sjs1"
  override val versionDefault: Version = Version("1.19.0")

  override def scalaCompileParameters(isScala3: Boolean): Seq[String] = if !isScala3 then Seq.empty else Seq("-scalajs")
  override def areCompilerPluginsBuiltIntoScala3: Boolean = true
  override def junit4: ScalaDependency.Maker = JUnit4ScalaJS
  override def versionExtractor(version: Version): Version = version
  override def versionComposer(projectScalaVersion: Version, backendVersion: Version): Version = backendVersion.simple

  private val group: String = "org.scala-js"

  private sealed class Maker(
    final override val artifact: String,
    what: String,
    final override val isScalaVersionFull: Boolean = false
  ) extends ScalaDependency.MakerScala2Jvm:
    final override def description: String = describe(what)
    final override def versionDefault: Version = ScalaJSBackend.versionDefault
    final override def group: String = ScalaJSBackend.group

  override def implementation: Array[ScalaDependency.Maker] = Array.empty
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
    override def group: String = ScalaJSBackend.group
    override val artifact: String = "scalajs-env-jsdom-nodejs"
    override val description: String = describe("Library for DOM manipulations on Node.js")

  object DomSJS extends ScalaDependency.Maker:
    override val versionDefault: Version = Version("2.8.0")
    override def group: String = ScalaJSBackend.group
    override val artifact: String = "scalajs-dom"
    override val description: String = describe("Library for DOM manipulations")

  override def additionalPluginDependencyRequirements: Array[DependencyRequirement[ScalaPlatform]] = Array(
    // ScalaModules.ParallelCollections,
    JSDomNodeJS.required()
  )

  override def additionalImplementationDependencyRequirements(
    backendVersion: Version,
    scalaVersion: Version,
    isScala3: Boolean
  ): Array[DependencyRequirement[ScalaPlatform]] =
    if !isScala3 // ++ is from IterableOnce and thus is not available on Scala 2.12...
    then Array(DomSJS.required()) 
    else Array(DomSJS.required(), ScalaVersion.Scala3.ScalaLibraryJS.required(scalaVersion)) // only for Scala 3
