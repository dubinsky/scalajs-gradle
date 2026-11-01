package org.podval.tools.test.framework

import org.podval.tools.build.{ScalaTestFramework, Version}

object Scalaprops extends ScalaTestFramework(
  name = "Scalaprops",
  nameSbt = "Scalaprops",
  group = "com.github.scalaprops",
  artifact = "scalaprops",
  versionDefault = Version("0.12.0"),
  className = "scalaprops.ScalapropsFramework",
  sharedPackages = List("scalaprops")
)
