package org.podval.tools.testing.framework

import org.podval.tools.testing.worker.TestTagsFilter

// https://github.com/com-lihaoyi/utest
// https://github.com/com-lihaoyi/utest/blob/master/utest/src/utest/runner/Framework.scala
object UTest extends FrameworkDescriptor(
  name = "utest",
  displayName = "UTest",
  group = "com.lihaoyi",
  artifact = "utest",
  versionDefault = "0.8.4",
  className = "utest.runner.Framework",
  sharedPackages = List("utest.runner") // TODO more?
):
  override def args(testTagsFilter: TestTagsFilter): Seq[String] = Seq.empty
