package org.podval.tools.test.framework

import org.podval.tools.build.{ScalaBackend, Version}

object Scalaprops extends ScalaFramework(
  name = "Scalaprops",
  description = "Scalaprops",
  group = "com.github.scalaprops",
  artifact = "scalaprops",
  versionDefault = Version("0.10.0"),
  className = "scalaprops.ScalapropsFramework",
  sharedPackages = List("scalaprops"),
  tagOptions = None
):
  override def isBackendSupported(backend: ScalaBackend): Boolean = !backend.isNative && !backend.isJs
