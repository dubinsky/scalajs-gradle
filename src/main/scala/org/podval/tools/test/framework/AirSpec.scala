package org.podval.tools.test.framework

import org.podval.tools.build.{ScalaBackend, ScalaVersion, Version}

object AirSpec extends ScalaFramework(
  name = "airspec",
  description = "AirSpec",
  group = "org.wvlet.airframe",
  artifact = "airspec",
  versionDefault = Version("2025.1.16"),
  className = "wvlet.airspec.Framework",
  sharedPackages = List("wvlet.airspec"),
  tagOptions = None
):
  override def isBackendSupported(backend: ScalaBackend): Boolean = true

  // Note: Scala Native is supported only on Scala 3
  override def isBackendSupported(backend: ScalaBackend, scalaVersion: ScalaVersion): Boolean =
    if backend.isNative then scalaVersion.isScala3 else isBackendSupported(backend)
