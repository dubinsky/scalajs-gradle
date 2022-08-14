package org.podval.tools.test.framework

import org.podval.tools.test.TestTagsFilter

// https://github.com/typelevel/scalacheck
// https://github.com/typelevel/scalacheck/blob/main/core/shared/src/main/scala/org/scalacheck/ScalaCheckFramework.scala
object ScalaCheck extends FrameworkDescriptor(
  name = "ScalaCheck",
  implementationClassName = "org.scalacheck.ScalaCheckFramework"
):
  override def args(testTagsFilter: TestTagsFilter): Array[String] = Array.empty
