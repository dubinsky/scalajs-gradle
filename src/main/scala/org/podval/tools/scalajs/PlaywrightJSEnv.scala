package org.podval.tools.scalajs

import org.podval.tools.build.{ScalaDependencyMaker, Version}

object PlaywrightJSEnv extends ScalaDependencyMaker.JvmScala2:
  override def group: String = "io.github.gmkumar2005"
  override def artifact: String = "scala-js-env-playwright"
  override def versionDefault: Version = Version("0.1.18")
  override val description: String = ScalaJSBackend.describe("Playwright JavaScript environment")
