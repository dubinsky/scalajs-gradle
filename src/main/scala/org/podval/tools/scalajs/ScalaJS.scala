package org.podval.tools.scalajs

import org.podval.tools.build.{ScalaDependency, Version}

object ScalaJS:
  val versionDefault: Version = Version("1.19.0")

  private val group: String = "org.scala-js"
  
  sealed class Maker(
    final override val artifact: String,
    what: String,
    final override val isScalaVersionFull: Boolean = false
  ) extends ScalaDependency.MakerScala2Jvm:
    final override def description: String = s"Scala.js $what."
    final override def versionDefault: Version = ScalaJS.versionDefault
    final override def group: String = ScalaJS.group

  // compiler plugins for Scala 2
  object Compiler extends Maker(
    "scalajs-compiler", 
    "Compiler Plugin for compiling Scala.js on Scala 2",
    isScalaVersionFull = true
  )
  
  object JUnitPlugin extends Maker(
    "scalajs-junit-test-plugin", 
    "JUnit4 Compiler Plugin for generating JUnit4 bootstrappers on Scala 2",
    isScalaVersionFull = true
  )
  
  object Library     extends Maker("scalajs-library"         , "Library")
  object Linker      extends Maker("scalajs-linker"          , "Linker for linking Scala.js code")
  object TestBridge  extends Maker("scalajs-test-bridge"     , "Test Bridge for testing Scala.js code"):
    override def useExactVersionInVerifyRequired: Boolean = true
    
  object TestAdapter extends Maker("scalajs-sbt-test-adapter", "Test Adapter for running the tests on Node.js")
  
//  object TestInterface extends Maker("scalajs-test-interface")
  
  object JSDomNodeJS extends ScalaDependency.MakerScala2Jvm:
    override val versionDefault: Version = Version("1.1.0")
    override def group: String = ScalaJS.group
    override def artifact: String = "scalajs-env-jsdom-nodejs"
    override def description: String = "Scala.js Library for DOM manipulations on Node.js."

  object DomSJS extends ScalaDependency.Maker:
    override def versionDefault: Version = Version("2.8.0")
    override def group: String = ScalaJS.group
    override def artifact: String = "scalajs-dom"
    override def description: String = "Scala.js Library for DOM manipulations."
