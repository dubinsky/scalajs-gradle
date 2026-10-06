package org.podval.tools.test.framework

import org.podval.tools.build.{ScalaTestFramework, Version}

// https://github.com/com-lihaoyi/utest
// https://github.com/com-lihaoyi/utest/blob/master/utest/src/utest/runner/Framework.scala
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
