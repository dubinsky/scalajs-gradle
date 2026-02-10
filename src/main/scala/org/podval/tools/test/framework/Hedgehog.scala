package org.podval.tools.test.framework

import org.podval.tools.build.{ScalaTestFramework, Version}

object Hedgehog extends ScalaTestFramework(
  name = "Hedgehog",
  nameSbt = "Hedgehog",
  group = "qa.hedgehog",
  artifact = "hedgehog-sbt",
  versionDefault = Version("0.13.0"),
  className = "hedgehog.sbt.Framework",
  sharedPackages = List("hedgehog")
)
