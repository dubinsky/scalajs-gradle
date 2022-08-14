package org.podval.tools.test.framework

import org.podval.tools.test.TestTagsFilter

// https://github.com/com-lihaoyi/utest
// https://github.com/com-lihaoyi/utest/blob/master/utest/src/utest/runner/Framework.scala
object UTest extends FrameworkDescriptor(
  name = "utest",
  implementationClassName = "utest.runner.Framework"
):
  override def args(testTagsFilter: TestTagsFilter): Array[String] = Array.empty
