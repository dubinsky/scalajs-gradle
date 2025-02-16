package org.podval.tools.scalajs

import org.podval.tools.build.{ScalaDependency, Version}

object ScalaJS:
  val versionDefault: Version = Version("1.18.2")

  private val group: String = "org.scala-js"
  
  sealed class Maker(
    final override val artifact: String,
    final override val isScalaVersionFull: Boolean = false
  ) extends ScalaDependency.MakerScala2Jvm:
    final override def versionDefault: Version = ScalaJS.versionDefault
    final override def group: String = ScalaJS.group
    
  object Library       extends Maker("scalajs-library")
  object Compiler      extends Maker("scalajs-compiler", isScalaVersionFull = true)
  object Linker        extends Maker("scalajs-linker")
  object TestBridge    extends Maker("scalajs-test-bridge")
  object TestAdapter   extends Maker("scalajs-sbt-test-adapter")
  object TestInterface extends Maker("scalajs-test-interface")
  
  object JSDomNodeJS extends ScalaDependency.MakerScala2Jvm:
    override val versionDefault: Version = Version("1.1.0")
    override def group: String = ScalaJS.group
    override def artifact: String = "scalajs-env-jsdom-nodejs"

  object DomSJS extends ScalaDependency.Maker:
    override def versionDefault: Version = Version("2.8.0")
    override def group: String = ScalaJS.group
    override def artifact: String = "scalajs-dom"
