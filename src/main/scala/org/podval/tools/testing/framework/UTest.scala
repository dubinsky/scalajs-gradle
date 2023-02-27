package org.podval.tools.testing.framework

import org.podval.tools.testing.worker.TestTagsFilter

// https://github.com/com-lihaoyi/utest
// https://github.com/com-lihaoyi/utest/blob/master/utest/src/utest/runner/Framework.scala
// brings in test-interface
object UTest extends FrameworkDescriptor(
  name = "utest",
  implementationClassName = "utest.runner.Framework"
):
  override def args(testTagsFilter: TestTagsFilter): Array[String] = Array.empty
