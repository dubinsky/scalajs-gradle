package org.podval.tools.test.framework

import org.podval.tools.build.{ScalaBackend, ScalaDependency, Version}

object WeaverTest extends ScalaFramework(
  name = "weaver-cats-effect",
  description = "Weaver-test",
  group = "org.typelevel",
  artifact = "weaver-cats",
  versionDefault = Version("0.10.1"),
  className = "weaver.framework.CatsEffect",
  sharedPackages = List("weaver"),
  tagOptions = None
):
  override def isBackendSupported(backend: ScalaBackend): Boolean =
    !backend.isNative && !backend.isJs
