package org.podval.tools.test.framework

import org.podval.tools.test.TestTagsFilter

// http://etorreborre.github.io/specs2/
// https://github.com/etorreborre/specs2
// https://github.com/etorreborre/specs2/blob/main/core/shared/src/main/scala/org/specs2/runner/Specs2Framework.scala
// Specs (org.specs.runner.SpecsFramework) is deprecated; use Specs2.
object Specs2 extends FrameworkDescriptor(
  name = "specs2",
  implementationClassName = "org.specs2.runner.Specs2Framework"
):
  override def args(testTagsFilter: TestTagsFilter): Array[String] = Array.empty
