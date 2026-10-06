package org.podval.tools.test.framework

import org.podval.tools.build.{ScalaTestFramework, TagOptions, Version}

// implementation: https://github.com/scalatest/scalatest/blob/main/jvm/core/src/main/scala/org/scalatest/tools/Framework.scala
// runner arguments: https://www.scalatest.org/user_guide/using_the_runner
// No nested tasks.
// DOES NOT bring in test-interface (in non-Scala.js variant)!
object ScalaTest extends ScalaTestFramework(
  name = "ScalaTest",
  nameSbt = "ScalaTest",
  group = "org.scalatest",
  artifact = "scalatest",
  versionDefault = Version("3.2.20"),
  className = "org.scalatest.tools.Framework",
  sharedPackages = List("org.scalatest"),
  tagOptions = TagOptions.OptionPerValue("-n", "-l")
)
