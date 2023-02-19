package org.podval.tools.scalajs

import org.opentorah.build.{Scala2Dependency, Scala3Dependency}

object ScalaJS:
  val configurationName: String = "scalajs"

  val group: String = "org.scala-js"
  val versionDefault: String = "1.13.0"

  // Note: no Scala 3 flavours exists
  object Library     extends Scala2Dependency(group = group, nameBase = "scalajs-library")
  object TestBridge  extends Scala2Dependency(group = group, nameBase = "scalajs-test-bridge")
  object Linker      extends Scala2Dependency(group = group, nameBase = "scalajs-linker")
  object TestAdapter extends Scala2Dependency(group = group, nameBase = "scalajs-sbt-test-adapter")
  object Compiler    extends Scala2Dependency(group = group, nameBase = "scalajs-compiler", isScala2versionFull = true)

  object JSDomNodeJS extends Scala2Dependency(group = group, nameBase = "scalajs-env-jsdom-nodejs"):
    val versionDefault: String = "1.1.0"

  object DomSJS:
    val nameBase: String = "scalajs-dom_sjs1"
    val versionDefault: String = "2.4.0"

    object Scala2 extends Scala2Dependency(group = group, nameBase = nameBase)
    object Scala3 extends Scala3Dependency(group = group, nameBase = nameBase)
