package org.podval.tools.test.framework

import org.podval.tools.test.TestTagging

// https://github.com/typelevel/scalacheck
// https://github.com/typelevel/scalacheck/blob/main/core/shared/src/main/scala/org/scalacheck/ScalaCheckFramework.scala
object ScalaCheck extends FrameworkDescriptor(
  name = "ScalaCheck",
  implementationClassName = "org.scalacheck.ScalaCheckFramework"
):
  override def args(testTagging: TestTagging): Array[String] = Array.empty

