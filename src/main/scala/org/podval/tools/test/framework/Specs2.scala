package org.podval.tools.test.framework

import org.podval.tools.build.{ScalaTestFramework, ScalaVersion, TagOptions, Version}

// http://etorreborre.github.io/specs2/
// https://github.com/etorreborre/specs2
// https://github.com/etorreborre/specs2/blob/main/core/shared/src/main/scala/org/specs2/runner/Specs2Framework.scala
// Specs (org.specs.runner.SpecsFramework) is deprecated; use Specs2.
object Specs2 extends ScalaTestFramework(
  name = "Specs2",
  nameSbt = "specs2",
  group = "org.specs2",
  artifact = "specs2-core",
  versionDefault = Version("5.11.0"),
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
  // Latest version that supports Scala 2; v5 doesn't support it...
  val versionDefaultScala2: Version = Version("4.23.0")

  // 5.10.0 and later depend on scala3-library 3.9.0 (TASTy 28.9).
  // Scala 3.7 reads stable TASTy through 28.7, so that library does not compile there.
  val versionDefaultBeforeScala3_9: Version = Version("5.9.1")

  private val scala3_9: Version = Version("3.9.0")

  override def versionDefault(scalaVersion: ScalaVersion): Option[Version] =
    if !scalaVersion.binaryVersion.isScala3 then Some(versionDefaultScala2)
    else if scalaVersion.version >= scala3_9 then None
    else Some(versionDefaultBeforeScala3_9)
