package org.podval.tools.scalajs

import org.podval.tools.build.{ScalaDependencyMaker, Version}

object DomSJS extends ScalaDependencyMaker:
  override val versionDefault: Version = Version("2.8.0")
  override def group: String = ScalaJSBackend.group
  override val artifact: String = "scalajs-dom"
  override val description: String = ScalaJSBackend.describe("Library for DOM manipulations")
  override def scalaBackend: ScalaJSBackend.type = ScalaJSBackend
