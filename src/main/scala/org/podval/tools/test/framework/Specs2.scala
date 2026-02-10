package org.podval.tools.test.framework

import org.podval.tools.build.{Backend, ScalaBinaryVersion, ScalaLibrary, ScalaTestFramework, TagOptions, Version}
import org.podval.tools.scalanative.ScalaNativeBackend

// http://etorreborre.github.io/specs2/
// https://github.com/etorreborre/specs2
// https://github.com/etorreborre/specs2/blob/main/core/shared/src/main/scala/org/specs2/runner/Specs2Framework.scala
// Specs (org.specs.runner.SpecsFramework) is deprecated; use Specs2.

// Dependencies:
// Scala:
// org.specs2:specs2-core_3
//   org.specs2:specs2-common_3
//   org.specs2:specs2-matcher_3
//   org.specs2:specs2-fp_3
//   org.portable-scala:portable-scala-reflect_2.13
//   org.scala-sbt:test-interface:1.0
// also:
//   org.scala-lang:scala3-library_3
//   org.scala-lang:scala-library:2.13.x
//
// Scala.js:
// org.specs2:specs2-core_sjs1_3
//   org.specs2:specs2-common_sjs1_3
//   org.specs2:specs2-matcher_sjs1_3
//   org.specs2:specs2-fp_sjs1_3
//   org.portable-scala:portable-scala-reflect_sjs1_2.13
//   org.scala-js:scala-js-macrotask-executor_sjs1_3
//   org.scala-sbt:test-interface:1.0
//   org.scala-js:scalajs-test-interface_2.13
// also:
//   org.scala-lang:scala-library:2.13.x

object Specs2 extends ScalaTestFramework(
  name = "Specs2",
  nameSbt = "specs2",
  group = "org.specs2",
  artifact = "specs2-core",
  versionDefault = Version("5.7.0"),
  className = "org.specs2.runner.Specs2Framework",
  sharedPackages = List("org.specs2.runner"),
  tagOptions = TagOptions.ListWithoutEq("include", "exclude"),
  additionalOptions = Array(
    // specs2 writes "stats" into a "$project/target/specs2-reports/stats", which makes sense for sbt
    // (on JVM; on non-JVM, it writes them into a memory store);
    // changing it to something that makes sense for Gradle;
    // location is hard-coded and thus not affected by changes to the project layout:
    "stats.outdir", "build/reports/tests/specs2-reports/stats"
  )
):
  // Latest version that supports Scala 2 *and* Scala Native; v5 doesn't support either...
  val versionDefaultScala2: Version = Version("4.23.0")

  override def versionDefault(backend: Backend, scalaLibrary: ScalaLibrary): Option[Version] =
    val isScala2: Boolean = scalaLibrary.scalaVersion.binaryVersion match
      case _: ScalaBinaryVersion.Scala2 => true
      case _ => false
    if isScala2 || backend == ScalaNativeBackend
    then Some(versionDefaultScala2)
    else None
