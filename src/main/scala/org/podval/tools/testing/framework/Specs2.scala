package org.podval.tools.testing.framework

import org.podval.tools.testing.worker.TestTagsFilter

// http://etorreborre.github.io/specs2/
// https://github.com/etorreborre/specs2
// https://github.com/etorreborre/specs2/blob/main/core/shared/src/main/scala/org/specs2/runner/Specs2Framework.scala
// Specs (org.specs.runner.SpecsFramework) is deprecated; use Specs2.
// brings in test-interface
object Specs2 extends FrameworkDescriptor(
  name = "specs2",
  implementationClassName = "org.specs2.runner.Specs2Framework"
):
  override def args(testTagsFilter: TestTagsFilter): Array[String] = Array.empty
