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
  val versionDefault: Version = Version("1.19.0")

  object DomSJS extends ScalaJSDependency("scalajs-dom", "Library for DOM manipulations"):
    override def scalaBackend: ScalaJSBackend.type = ScalaJSBackend
    override val versionDefault: Version = Version("2.8.0")

  object JSDomNodeJSEnv 
    extends ScalaJSDependency("scalajs-env-jsdom-nodejs", "Node.js JavaScript environment with JSDOM")
    with ScalaDependencyMaker.NotPublishedForScala3:
    override def scalaBackend: JvmBackend.type = JvmBackend
    override val versionDefault: Version = Version("1.1.0")

  sealed class Jvm(artifact: String, what: String)
    extends ScalaJSDependency(artifact, what)
    with ScalaDependencyMaker.NotPublishedForScala3:
    final override def scalaBackend: JvmBackend.type = JvmBackend
    final override def versionDefault: Version = ScalaJSDependency.versionDefault

  object Library      extends Jvm("scalajs-library", "Library")
  object Linker       extends Jvm("scalajs-linker", "Linker")
  object TestAdapter  extends Jvm("scalajs-sbt-test-adapter", "Test Adapter for Node.js")
  
  // object TestInterface extends Jvm("scalajs-test-interface")
  
  object TestBridge   extends Jvm("scalajs-test-bridge", "Test Bridge for Node.js"):
    override def isDependencyRequirementVersionExact: Boolean = true

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

  object Scala3LibraryJS extends ScalaDependencyMaker:
    override def scalaBackend: ScalaJSBackend.type = ScalaJSBackend
    override def versionDefault: Version = ScalaBinaryVersion.Scala3.versionDefault
    override def group: String = ScalaBinaryVersion.group
    override def artifact: String = "scala3-library"
    override def description: String = "Scala 3 library in Scala.js."

    // There is no Scala 2 equivalent
    override def isPublishedFor(binaryVersion: ScalaBinaryVersion): Boolean = binaryVersion match
      case ScalaBinaryVersion.Scala3 => true
      case _ => false
  
  object PlaywrightJSEnv extends ScalaDependencyMaker.NotPublishedForScala3:
    override def scalaBackend: JvmBackend.type = JvmBackend
    override def group: String = "io.github.gmkumar2005"
    override def artifact: String = "scala-js-env-playwright"
    override def versionDefault: Version = Version("0.1.18")
    override val description: String = ScalaJSBackend.describe("Playwright JavaScript environment")
