package org.podval.tools.test.framework

import org.podval.tools.build.{ScalaBackend, Version}

// https://github.com/zio/zio/blob/series/2.x/test-sbt/jvm/src/main/scala/zio/test/sbt/ZTestFramework.scala
// https://github.com/zio/zio/blob/series/2.x/test/shared/src/main/scala/zio/test/TestArgs.scala
//  -t            testSearchTerm list
//  -tags         list
//  -ignore-tags  list
//  -policy       testTaskPolicy - ignored
//  -renderer     summary renderer; "intellij" or nothing; when IntelliJ, result code is always 0
//  -summary      print summary or not: flag

// Dependencies:
// Scala:
// dev.zio:zio-test-sbt_3
//   dev.zio:zio-test_3
//   dev.zio:zio_3
//   dev.zio:zio-internal-macros_3
//   dev.zio:zio-stacktracer_3
//   dev.zio:zio-streams_3
//   dev.zio:izumi-reflect_3
//   dev.zio:izumi-reflect-thirdparty-boopickle-shaded_3
//   org.portable-scala:portable-scala-reflect_2.13
// also:
//   org.scala-sbt:test-interface:1.0
//   org.scala-lang:scala3-library_3
//
// Scala.js:
// dev.zio:zio-test-sbt_sjs1_3
//   dev.zio:zio-test_sjs1_3
//   dev.zio:zio_sjs1_3
//   dev.zio:zio-internal-macros_sjs1_3
//   dev.zio:zio-stacktracer_sjs1_3
//   dev.zio:zio-streams_sjs1_3
//   dev.zio:izumi-reflect_sjs1_3
//   dev.zio:izumi-reflect-thirdparty-boopickle-shaded_sjs1_3
//   org.portable-scala:portable-scala-reflect_sjs1_2.13
//   org.scala-js:scala-js-macrotask-executor_sjs1_3
//   org.scala-js:scalajs-weakreferences_sjs1_2.13
//   org.scala-js:scalajs-dom_sjs1_3
//   io.github.cquiroz:scala-java-time_sjs1_3
//   io.github.cquiroz:scala-java-locales_sjs1_3
//   io.github.cquiroz:cldr-api_sjs1_3
//   io.github.cquiroz:scala-java-time-tzdb_sjs1_3
//   org.scala-js:scalajs-test-interface_2.13
// also:
//   org.scala-lang:scala3-library_sjs1_3
//   org.scala-lang:scala-library:2.13.x
//   org.scala-js:scalajs-library_2.13

// Note: DO NOT supply "-renderer intellij" argument:
// zio.test.render.IntelliJRenderer emits "##teamcity[" messages!
object ZioTest extends ScalaFramework(
  // This must be exactly as reported by the framework!
  name = s"${io.AnsiColor.UNDERLINED}ZIO Test${io.AnsiColor.RESET}",
  description = "ZioTest",
  group = "dev.zio",
  artifact = "zio-test-sbt",
  versionDefault = Version("2.1.20"),
  className = "zio.test.sbt.ZTestFramework",
  sharedPackages = List("zio.test.sbt"),
  tagOptions = TagOptions.OptionPerValue("-tags", "-ignore-tags")
):
  override def isBackendSupported(backend: ScalaBackend): Boolean = true

  override def additionalOptions: Array[String] = Array(
    // ZIO Test writes test report "test-reports-zio/output.json" into "target", which makes sense for sbt;
    // changing it to something that makes sense for Gradle;
    // location is hard-coded and thus not affected by changes to the project layout:
    "-reports", "build/reports/tests"
  )
