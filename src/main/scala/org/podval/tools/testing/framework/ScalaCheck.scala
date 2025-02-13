package org.podval.tools.testing.framework

import org.podval.tools.build.Version
import org.podval.tools.testing.worker.TestTagsFilter

// https://github.com/typelevel/scalacheck
// https://github.com/typelevel/scalacheck/blob/main/core/shared/src/main/scala/org/scalacheck/ScalaCheckFramework.scala
// Dependencies:
// Scala:
// org.scalacheck:scalacheck_3:1.18.1
//   org.scala-sbt:test-interface:1.0
// also:
//   org.scala-lang:scala3-library_3:
//
// ScalaJS:
// org.scalacheck:scalacheck_sjs1_3:1.18.1
//   org.scala-js:scalajs-test-interface_2.13
// also:
//   org.scala-lang:scala3-library_sjs1_3
//   org.scala-js:scalajs-library_2.13
object ScalaCheck extends FrameworkDescriptor(
  name = "ScalaCheck",
  displayName = "ScalaCheck",
  group = "org.scalacheck",
  artifact = "scalacheck",
  versionDefault = Version("1.18.1"),
  className = "org.scalacheck.ScalaCheckFramework",
  sharedPackages = List("org.scalacheck")
):
  override def args(testTagsFilter: TestTagsFilter): Seq[String] = Seq.empty
