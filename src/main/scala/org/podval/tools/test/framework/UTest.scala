package org.podval.tools.test.framework

import org.podval.tools.build.{ScalaTestFramework, Version}

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
object UTest extends ScalaTestFramework(
  name = "UTest",
  nameSbt = "utest",
  group = "com.lihaoyi",
  artifact = "utest",
  versionDefault = Version("0.9.5"),
  className =
  //"utest.runner.MillFramework", // logs progress, but writes summary to standard out
    "utest.runner.Framework", // returns correct summary, but writes progress to standard out
  // we are better off with the one that returns the actual summary:
  // anyway, `MUnit` and `ZIO Test` also write progress to standard out ;)
  sharedPackages = List("utest.runner")
)
