package org.podval.tools.test.framework

import org.podval.tools.test.TestTagging

// https://github.com/com-lihaoyi/utest
// https://github.com/com-lihaoyi/utest/blob/master/utest/src/utest/runner/Framework.scala
object UTest extends FrameworkDescriptor(
  name = "utest",
  implementationClassName = "utest.runner.Framework"
):
  override def args(testTagging: TestTagging): Array[String] = Array.empty
