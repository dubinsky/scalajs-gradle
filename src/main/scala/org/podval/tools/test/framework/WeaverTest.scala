package org.podval.tools.test.framework

import org.podval.tools.build.{ScalaTestFramework, Version}

object WeaverTest extends ScalaTestFramework(
  name = "Weaver-test",
  nameSbt = "weaver-cats-effect",
  group = "org.typelevel",
  artifact = "weaver-cats",
  versionDefault = Version("0.12.0"),
  className = "weaver.framework.CatsEffect",
  sharedPackages = List("weaver")
)
