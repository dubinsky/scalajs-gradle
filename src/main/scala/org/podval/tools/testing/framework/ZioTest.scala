package org.podval.tools.testing.framework

import org.podval.tools.testing.worker.TestTagsFilter

// https://github.com/zio/zio/blob/series/2.x/test-sbt/jvm/src/main/scala/zio/test/sbt/ZTestFramework.scala
object ZioTest extends FrameworkDescriptor(
  // Note: this must be exactly as reported by the framework:
  name = s"${io.AnsiColor.UNDERLINED}ZIO Test${io.AnsiColor.RESET}",
  displayName = "ZioTest",
  group = "dev.zio",
  artifact = "zio-test-sbt",
  versionDefault = "2.1.14",
  className = "zio.test.sbt.ZTestFramework",
  sharedPackages = List("zio.test.sbt") // TODO more?
):
  // https://github.com/zio/zio/blob/series/2.x/test/shared/src/main/scala/zio/test/TestArgs.scala
  //  -t            testSearchTerm list
  //  -tags         list
  //  -ignore-tags  list
  //  -policy       testTaskPolicy - ignored
  //  -renderer     summary renderer; "intellij" or nothing; when IntelliJ, result code is always 0
  //  -summary      print summary or not: flag
  override def args(testTagsFilter: TestTagsFilter): Seq[String] =
    FrameworkDescriptor.listOfOptions(       "-tags", testTagsFilter.include) ++
    FrameworkDescriptor.listOfOptions("-ignore-tags", testTagsFilter.exclude)
