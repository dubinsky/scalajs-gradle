package org.podval.tools.test.framework

import org.podval.tools.build.{ScalaBackend, ScalaBinaryVersion, ScalaVersion, Version}
import org.podval.tools.scalanative.ScalaNativeBackend

object AirSpec extends ScalaFramework(
  name = "airspec",
  description = "AirSpec",
  group = "org.wvlet.airframe",
  artifact = "airspec",
  versionDefault = Version("2026.1.0"),
  className = "wvlet.airspec.Framework",
  sharedPackages = List("wvlet.airspec")
):
  override def isBackendSupported(backend: ScalaBackend): Boolean = true

  // Note: Scala Native is supported only on Scala 3
  override def isBackendSupported(backend: ScalaBackend, scalaVersion: ScalaVersion): Boolean = backend match
    case ScalaNativeBackend => scalaVersion.binaryVersion match
      case _: ScalaBinaryVersion.Scala3 => true
      case _ => false
    case _ => isBackendSupported(backend)
