package org.podval.tools.testing.framework

import org.podval.tools.testing.worker.TestTagsFilter

// https://github.com/zio/zio/blob/series/2.x/test-sbt/jvm/src/main/scala/zio/test/sbt/ZTestFramework.scala
object ZIOTest extends FrameworkDescriptor(
  name = "ZIO Test",
  implementationClassName = "zio.test.sbt.ZTestFramework"
):
  override def args(testTagsFilter: TestTagsFilter): Array[String] = Array.empty
