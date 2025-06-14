package org.podval.tools.backend.scalajs

import org.podval.tools.backend.jvm.JvmBackend
import org.podval.tools.backend.nonjvm.NonJvmBackend
import org.podval.tools.build.{Dependency, DependencyRequirement, ScalaBinaryVersion, ScalaDependency, ScalaVersion,
  Version}
import org.podval.tools.test.framework.JUnit4ScalaJS

case object ScalaJSBackend extends NonJvmBackend:
  override val name: String = "Scala.js"
  override val sourceRoot: String = "js"
  override val artifactSuffix: String = "sjs1"
  override val versionDefault: Version.Simple = Version.Simple("1.19.0")

  override def scalaCompileParameters(scalaVersion: ScalaVersion): Seq[String] =
    if scalaVersion.isScala3 
    then Seq("-scalajs")
    else Seq.empty
    
  override def areCompilerPluginsBuiltIntoScala3: Boolean = true
  override def junit4: Dependency.Maker = JUnit4ScalaJS.forJS.get.maker
  override def versionExtractor(version: Version): Version.Simple = version.simple
  
  override def versionComposer(
    projectScalaVersion: ScalaVersion,
    backendVersion: Version.Simple
  ): Version = backendVersion.simple

  private val group: String = "org.scala-js"

  private sealed class Maker(
    final override val artifact: String,
    what: String,
    final override val isScalaVersionFull: Boolean = false
  ) extends ScalaDependency.Maker:
    final override def description: String = describe(what)
    final override def versionDefault: Version.Simple = ScalaJSBackend.versionDefault
    final override def group: String = ScalaJSBackend.group
    final override def scalaBackend: JvmBackend.type = JvmBackend
    final override def scala2: Boolean = true

  override def implementation: Array[ScalaDependency.Maker] = Array.empty
  override def library(scalaVersion: ScalaVersion): ScalaDependency.Maker = Maker("scalajs-library", "Library")
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

  object JSDomNodeJS extends ScalaDependency.Maker:
    override val versionDefault: Version.Simple = Version.Simple("1.1.0")
    override def group: String = ScalaJSBackend.group
    override val artifact: String = "scalajs-env-jsdom-nodejs"
    override val description: String = describe("Library for DOM manipulations on Node.js")
    override def scalaBackend: JvmBackend.type = JvmBackend
    override def scala2: Boolean = true

  object DomSJS extends ScalaDependency.Maker:
    override val versionDefault: Version.Simple = Version.Simple("2.8.0")
    override def group: String = ScalaJSBackend.group
    override val artifact: String = "scalajs-dom"
    override val description: String = describe("Library for DOM manipulations")
    override def scalaBackend: ScalaJSBackend.type = ScalaJSBackend

  override def additionalPluginDependencyRequirements: Array[DependencyRequirement] = Array(
    // ScalaModules.ParallelCollections,
    JSDomNodeJS.required()
  )

  override def additionalImplementationDependencyRequirements(
    backendVersion: Version,
    scalaVersion: ScalaVersion
  ): Array[DependencyRequirement] =
    if !scalaVersion.isScala3 // ++ is from IterableOnce and thus is not available on Scala 2.12...
    then Array(DomSJS.required()) 
    else Array(DomSJS.required(), ScalaBinaryVersion.Scala3.ScalaLibraryJS.required(scalaVersion.version)) // only for Scala 3
