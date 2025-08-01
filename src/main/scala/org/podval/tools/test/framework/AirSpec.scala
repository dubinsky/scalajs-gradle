package org.podval.tools.test.framework

import org.podval.tools.build.{ScalaBackend, ScalaVersion, Version}

object AirSpec extends ScalaFramework(
  name = "airspec",
  description = "AirSpec",
  group = "org.wvlet.airframe",
  artifact = "airspec",
  versionDefault = Version("2025.1.14"),
  className = "wvlet.airspec.Framework",
  sharedPackages = List("wvlet.airspec"),
  tagOptions = None
):
  // Note: Scala Native is supported - but only on Scala 3!
  override def isBackendSupported(backend: ScalaBackend, scalaVersion: ScalaVersion): Boolean =
    if backend.isNative then scalaVersion.isScala3 else isBackendSupported(backend)

  override def isBackendSupported(backend: ScalaBackend): Boolean = true
