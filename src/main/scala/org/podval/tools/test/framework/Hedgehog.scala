package org.podval.tools.test.framework

import org.podval.tools.build.{ScalaBackend, Version}

object Hedgehog extends ScalaFramework(
  name = "Hedgehog",
  description = "Hedgehog",
  group = "qa.hedgehog",
  artifact = "hedgehog-sbt",
  versionDefault = Version("0.13.0"),
  className = "hedgehog.sbt.Framework",
  sharedPackages = List("hedgehog"),
  tagOptions = None
):
  override def isBackendSupported(backend: ScalaBackend): Boolean = true
