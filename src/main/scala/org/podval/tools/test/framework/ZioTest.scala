package org.podval.tools.test.framework

import org.podval.tools.build.{ScalaTestFramework, TagOptions, Version}

// https://github.com/zio/zio/blob/series/2.x/test-sbt/jvm/src/main/scala/zio/test/sbt/ZTestFramework.scala
// https://github.com/zio/zio/blob/series/2.x/test/shared/src/main/scala/zio/test/TestArgs.scala
//  -t            testSearchTerm list
//  -tags         list
//  -ignore-tags  list
//  -policy       testTaskPolicy - ignored
//  -renderer     summary renderer; "intellij" or nothing; when IntelliJ, result code is always 0
//  -summary      print summary or not: flag

// Note: DO NOT supply "-renderer intellij" argument:
// zio.test.render.IntelliJRenderer emits "##teamcity[" messages!
object ZioTest extends ScalaTestFramework(
  name = "ZioTest",
  nameSbt = s"${io.AnsiColor.UNDERLINED}ZIO Test${io.AnsiColor.RESET}",
  group = "dev.zio",
  artifact = "zio-test-sbt",
  versionDefault = Version("2.1.26"),
  className = "zio.test.sbt.ZTestFramework",
  sharedPackages = List("zio.test.sbt"),
  tagOptions = TagOptions.OptionPerValue("-tags", "-ignore-tags"),
  additionalOptions = Array(
    // ZIO Test writes test report "test-reports-zio/output.json" into "target", which makes sense for sbt;
    // changing it to something that makes sense for Gradle;
    // location is hard-coded and thus not affected by changes to the project layout:
    "-reports", "build/reports/tests"
  )
)
