package org.podval.tools.testing.framework

import org.podval.tools.testing.worker.TestTagsFilter

// https://github.com/typelevel/scalacheck
// https://github.com/typelevel/scalacheck/blob/main/core/shared/src/main/scala/org/scalacheck/ScalaCheckFramework.scala
// brings in test-interface
object ScalaCheck extends FrameworkDescriptor(
  name = "ScalaCheck",
  implementationClassName = "org.scalacheck.ScalaCheckFramework"
):
  override def args(testTagsFilter: TestTagsFilter): Seq[String] = Seq.empty
