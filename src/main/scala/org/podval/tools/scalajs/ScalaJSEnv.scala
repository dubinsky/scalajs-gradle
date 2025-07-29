package org.podval.tools.scalajs

import org.podval.tools.build.Version
import org.podval.tools.nonjvm.NonJvmBackend.Dep

object ScalaJSEnv:
  val domVersion: Version = Version("2.8.1")
  
  def dom: Dep = Dep(
    "scalajs-dom",
    "Library for DOM manipulations",
    _.withVersionDefault(domVersion)
  )

  val jsDomNodeVersion: Version = Version("1.1.0")

  def jsDomNode: Dep = Dep(
    "scalajs-env-jsdom-nodejs",
    "Node.js JavaScript environment with JSDOM",
    _
      .withVersionDefault(jsDomNodeVersion)
      .scala2
  )

  val playwrightVersion: Version = Version("0.1.18")

  def playwright: Dep = Dep(
    "scala-js-env-playwright",
    "Playwright JavaScript environment",
    _
      .withGroup("io.github.gmkumar2005")
      .withVersionDefault(playwrightVersion)
      .scala2
      .jvm
  )
