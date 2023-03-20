package org.podval.tools.testing.framework

import org.podval.tools.testing.worker.TestTagsFilter

// https://github.com/typelevel/scalacheck
// https://github.com/typelevel/scalacheck/blob/main/core/shared/src/main/scala/org/scalacheck/ScalaCheckFramework.scala
object ScalaCheck extends FrameworkDescriptor(
  name = "ScalaCheck",
  displayName = "ScalaCheck",
  group = "org.scalacheck",
  artifact = "scalacheck",
  versionDefault = "1.17.0",
  className = "org.scalacheck.ScalaCheckFramework",
  sharedPackages = List("org.scalacheck")
):
  override def args(testTagsFilter: TestTagsFilter): Seq[String] = Seq.empty
