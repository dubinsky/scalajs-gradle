package org.podval.tools.testing.framework

import org.podval.tools.build.Version
import org.podval.tools.testing.worker.TestTagsFilter

// https://github.com/com-lihaoyi/utest
// https://github.com/com-lihaoyi/utest/blob/master/utest/src/utest/runner/Framework.scala
// Dependencies:
// Scala:
// com.lihaoyi:utest_3:0.8.5
//   org.scala-sbt:test-interface:1.0
// also:
//   org.scala-lang:scala3-library_3
//
// ScalaJS:
// com.lihaoyi:utest_sjs1_3:0.8.5
//   org.scala-js:scalajs-test-interface_2.13
//   org.portable-scala:portable-scala-reflect_sjs1_2.13
// also:
//   org.scala-lang:scala3-library_sjs1_3
//   org.scala-js:scalajs-library_2.13
object UTest extends FrameworkDescriptor(
  name = "utest",
  displayName = "UTest",
  group = "com.lihaoyi",
  artifact = "utest",
  versionDefault = Version("0.8.5"),
  className = "utest.runner.Framework",
  sharedPackages = List("utest.runner") // TODO more?
):
  override def args(testTagsFilter: TestTagsFilter): Seq[String] = Seq.empty
