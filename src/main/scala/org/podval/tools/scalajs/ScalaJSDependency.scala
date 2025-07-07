package org.podval.tools.scalajs

import org.podval.tools.build.{ScalaBinaryVersion, ScalaDependencyMaker, Version}
import org.podval.tools.jvm.JvmBackend

sealed abstract class ScalaJSDependency(
  final override val artifact: String,
  what: String
) extends ScalaDependencyMaker:
  final override def description: String = ScalaJSBackend.describe(what)
  final override def group: String = ScalaJSDependency.group

object ScalaJSDependency:
  val group: String = "org.scala-js"

  object DomSJS extends ScalaJSDependency("scalajs-dom", "Library for DOM manipulations"):
    override val versionDefault: Version = Version("2.8.0")
    override def scalaBackend: ScalaJSBackend.type = ScalaJSBackend

  object JSDomNodeJSEnv extends ScalaJSDependency("scalajs-env-jsdom-nodejs", "Node.js JavaScript environment with JSDOM"):
    override val versionDefault: Version = Version("1.1.0")
    override def scalaBackend: JvmBackend.type = JvmBackend
    override def isPublishedForScala3: Boolean = false

  sealed class Jvm(artifact: String, what: String) extends ScalaJSDependency(artifact, what):
    final override def scalaBackend: JvmBackend.type = JvmBackend
    final override def versionDefault: Version = ScalaJSBackend.versionDefault
    final override def isPublishedForScala3: Boolean = false

  object Library      extends Jvm("scalajs-library", "Library")
  object Linker       extends Jvm("scalajs-linker", "Linker")
  object TestAdapter  extends Jvm("scalajs-sbt-test-adapter", "Test Adapter for Node.js")
  
  // object TestInterface extends Jvm("scalajs-test-interface")
  
  object TestBridge   extends Jvm("scalajs-test-bridge", "Test Bridge for Node.js"):
    override def useExactVersionInVerifyRequired: Boolean = true

  // compiler plugins for Scala 2
  object Compiler     extends Jvm(
    "scalajs-compiler",
    "Compiler Plugin for Scala 2"
  ):
    override def isScalaVersionFull: Boolean = true

  object JUnit4Plugin extends Jvm(
    "scalajs-junit-test-plugin",
    "JUnit4 Compiler Plugin for generating bootstrappers for Scala 2"
  ):
    override def isScalaVersionFull: Boolean = true

  // There is no Scala 2 equivalent
  object Scala3LibraryJS extends ScalaDependencyMaker:
    override def versionDefault: Version = ScalaBinaryVersion.Scala3.versionDefault.version
    override def group: String = ScalaBinaryVersion.group
    override def artifact: String = "scala3-library"
    override def description: String = "Scala 3 library in Scala.js."
    override def scalaBackend: ScalaJSBackend.type = ScalaJSBackend

  object PlaywrightJSEnv extends ScalaDependencyMaker:
    override def group: String = "io.github.gmkumar2005"
    override def artifact: String = "scala-js-env-playwright"
    override def versionDefault: Version = Version("0.1.18")
    override val description: String = ScalaJSBackend.describe("Playwright JavaScript environment")
    override def scalaBackend: JvmBackend.type = JvmBackend
    override def isPublishedForScala3: Boolean = false
    override def isPublishedForScala213: Boolean = false
