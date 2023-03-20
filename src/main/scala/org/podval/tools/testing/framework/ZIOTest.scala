package org.podval.tools.testing.framework

import org.podval.tools.testing.worker.TestTagsFilter

// https://github.com/zio/zio/blob/series/2.x/test-sbt/jvm/src/main/scala/zio/test/sbt/ZTestFramework.scala
object ZIOTest extends FrameworkDescriptor(
  name = s"${io.AnsiColor.UNDERLINED}ZIO Test${io.AnsiColor.RESET}",
  displayName = "zio-test",
  group = "dev.zio",
  artifact = "zio-test-sbt",
  versionDefault = "2.0.10",
  className = "zio.test.sbt.ZTestFramework",
  sharedPackages = List("zio.test.sbt") // TODO more?
):
  override def args(testTagsFilter: TestTagsFilter): Seq[String] =
    FrameworkDescriptor.listOfOptions(       "-tags", testTagsFilter.include) ++
    FrameworkDescriptor.listOfOptions("-ignore-tags", testTagsFilter.exclude)
