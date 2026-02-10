package org.podval.tools.test.framework

import org.podval.tools.build.{Backend, ScalaBinaryVersion, ScalaTestFramework, ScalaVersion, Version}
import org.podval.tools.scalanative.ScalaNativeBackend

object AirSpec extends ScalaTestFramework(
  name = "AirSpec",
  nameSbt = "airspec",
  group = "org.wvlet.airframe",
  artifact = "airspec",
  versionDefault = Version("2026.1.4"),
  className = "wvlet.airspec.Framework",
  sharedPackages = List("wvlet.airspec")
):
  // Note: Scala Native is supported only on Scala 3
  override def isBackendSupported(backend: Backend, scalaVersion: ScalaVersion): Boolean = backend match
    case ScalaNativeBackend => scalaVersion.binaryVersion match
      case _: ScalaBinaryVersion.Scala3 => true
      case _ => false
    case _ => true
