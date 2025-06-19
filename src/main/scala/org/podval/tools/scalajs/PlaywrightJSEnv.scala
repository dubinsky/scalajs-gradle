package org.podval.tools.scalajs

import org.podval.tools.build.{ScalaDependencyMaker, Version}
import org.podval.tools.jvm.JvmBackend

object PlaywrightJSEnv extends ScalaDependencyMaker:
  override def group: String = "io.github.gmkumar2005"
  override def artifact: String = "scala-js-env-playwright"
  override def versionDefault: Version = Version("0.1.18")
  override val description: String = ScalaJSBackend.describe("Playwright JavaScript environment")
  override def scalaBackend: JvmBackend.type = JvmBackend
  override def isPublishedForScala3: Boolean = false
  override def isPublishedForScala213: Boolean = false
