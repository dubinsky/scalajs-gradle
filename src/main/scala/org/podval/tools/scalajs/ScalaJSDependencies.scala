package org.podval.tools.scalajs

import org.podval.tools.build.{ScalaDependency, ScalaPlatform, Version}

object ScalaJSDependencies:
  private val group: String = "org.scala-js"
  
  val versionDefault: Version = Version("1.18.2")

  // Note: no Scala 3 flavours exists
  val Library      : ScalaDependency = ScalaPlatform.Scala2.Jvm.dependency(group, "scalajs-library")
  val Compiler     : ScalaDependency = ScalaPlatform.Scala2.Jvm.dependency(group, "scalajs-compiler", isScalaVersionFull = true)
  val Linker       : ScalaDependency = ScalaPlatform.Scala2.Jvm.dependency(group, "scalajs-linker")
  val TestBridge   : ScalaDependency = ScalaPlatform.Scala2.Jvm.dependency(group, "scalajs-test-bridge")
  val TestAdapter  : ScalaDependency = ScalaPlatform.Scala2.Jvm.dependency(group, "scalajs-sbt-test-adapter")
  val TestInterface: ScalaDependency = ScalaPlatform.Scala2.Jvm.dependency(group, "scalajs-test-interface")
  
  object JSDomNodeJS:
    val dependency : ScalaDependency = ScalaPlatform.Scala2.Jvm.dependency(group, "scalajs-env-jsdom-nodejs")
    val versionDefault: Version = Version("1.1.0")

  object DomSJS:
    private val artifact: String = "scalajs-dom"
    val versionDefault: Version = Version("2.8.0")

    def apply(isScala3: Boolean): ScalaDependency =
      (if isScala3 then ScalaPlatform.Scala3.JS else ScalaPlatform.Scala2.JS)
        .dependency(group, artifact)
