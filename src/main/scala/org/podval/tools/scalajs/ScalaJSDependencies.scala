package org.podval.tools.scalajs

import org.podval.tools.build.{ScalaDependency, Version}

object ScalaJSDependencies:
  private val group: String = "org.scala-js"
  
  val versionDefault: Version = Version("1.18.2")

  // Note: no Scala 3 flavours exists
  object Library       extends ScalaDependency.Scala2(group, "scalajs-library")
  object Compiler      extends ScalaDependency.Scala2(group, "scalajs-compiler", isScalaVersionFull = true)
  object Linker        extends ScalaDependency.Scala2(group, "scalajs-linker")
  object TestBridge    extends ScalaDependency.Scala2(group, "scalajs-test-bridge")
  object TestAdapter   extends ScalaDependency.Scala2(group, "scalajs-sbt-test-adapter")
  object TestInterface extends ScalaDependency.Scala2(group, "scalajs-test-interface")

  object JSDomNodeJS   extends ScalaDependency.Scala2(group, "scalajs-env-jsdom-nodejs"):
    val versionDefault: Version = Version("1.1.0")

  object DomSJS:
    private val artifact: String = "scalajs-dom"
    val versionDefault: Version = Version("2.8.0")

    object Scala2 extends ScalaDependency.Scala2(group, artifact, isScalaJS = true)
    object Scala3 extends ScalaDependency.Scala3(group, artifact, isScalaJS = true)
