package org.podval.tools.test.framework

import org.podval.tools.build.Version

// https://github.com/com-lihaoyi/utest
// https://github.com/com-lihaoyi/utest/blob/master/utest/src/utest/runner/Framework.scala
// Dependencies:
// Scala:
// com.lihaoyi:utest_3
//   org.scala-sbt:test-interface:1.0
// also:
//   org.scala-lang:scala3-library_3
//
// Scala.js:
// com.lihaoyi:utest_sjs1_3
//   org.scala-js:scalajs-test-interface_2.13
//   org.portable-scala:portable-scala-reflect_sjs1_2.13
// also:
//   org.scala-lang:scala3-library_sjs1_3
//   org.scala-js:scalajs-library_2.13
object UTest extends ScalaFrameworkDescriptor(
  name = "utest",
  description = "UTest",
  group = "com.lihaoyi",
  artifact = "utest",
  versionDefault = Version("0.8.9"),
  // `utest.runner.Framework` logs using `println`; to force the use of the SBT logs:
  className = "utest.runner.MillFramework",
  sharedPackages = List("utest.runner"),
  tagOptions = None
)
