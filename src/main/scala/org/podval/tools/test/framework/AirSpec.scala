package org.podval.tools.test.framework

import org.podval.tools.build.{Backend, ScalaTestFramework, Version}
import org.podval.tools.scalanative.ScalaNativeBackend

object AirSpec extends ScalaTestFramework(
  name = "AirSpec",
  nameSbt = "airspec",
  group = "org.wvlet.airframe",
  artifact = "airspec",
  versionDefault = Version("2026.1.6"),
  className = "wvlet.airspec.Framework",
  sharedPackages = List("wvlet.airspec")
):
  // Note: Scala Native is supported only on Scala 3
  override def isBackendSupported(backend: Backend, isScala3: Boolean): Boolean = backend match
    case ScalaNativeBackend => isScala3
    case _ => true
