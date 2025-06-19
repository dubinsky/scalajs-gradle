package org.podval.tools.scalajs

import org.podval.tools.build.{ScalaDependencyMaker, Version}
import org.podval.tools.jvm.JvmBackend

object JSDomNodeJSEnv extends ScalaDependencyMaker:
  override val versionDefault: Version = Version("1.1.0")
  override def group: String = ScalaJSBackend.group
  override val artifact: String = "scalajs-env-jsdom-nodejs"
  override val description: String = ScalaJSBackend.describe("Node.js JavaScript environment with JSDOM")
  override def scalaBackend: JvmBackend.type = JvmBackend
  override def isPublishedForScala3: Boolean = false
