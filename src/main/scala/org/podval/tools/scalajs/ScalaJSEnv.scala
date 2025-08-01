package org.podval.tools.scalajs

import org.podval.tools.build.Version
import org.podval.tools.nonjvm.NonJvmBackend.Dep

object ScalaJSEnv:
  // TODO needed for AirFrame, which does not bring it in! file an issue...
  def javaLogging: Dep = Dep(
    "scalajs-java-logging",
    "Port of the java.util.logging API of JDK 8 for Scala.js",
    _.scala2,
    version = Some(Version("1.0.0"))
  )

  val domVersion: Version = Version("2.8.1")
  
  def dom: Dep = Dep(
    "scalajs-dom",
    "Library for DOM manipulations",
    version = Some(domVersion)
  )

  val jsDomNodeVersion: Version = Version("1.1.0")

  def jsDomNode: Dep = Dep(
    "scalajs-env-jsdom-nodejs",
    "Node.js JavaScript environment with JSDOM",
    _.scala2,
    version = Some(jsDomNodeVersion)
  )

  val playwrightVersion: Version = Version("0.1.18")

  def playwright: Dep = Dep(
    "scala-js-env-playwright",
    "Playwright JavaScript environment",
    _.scala2.jvm,
    group = Some("io.github.gmkumar2005"),
    version = Some(playwrightVersion)
  )
