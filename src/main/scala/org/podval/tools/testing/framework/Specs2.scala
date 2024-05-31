package org.podval.tools.testing.framework

import org.podval.tools.testing.worker.TestTagsFilter

// http://etorreborre.github.io/specs2/
// https://github.com/etorreborre/specs2
// https://github.com/etorreborre/specs2/blob/main/core/shared/src/main/scala/org/specs2/runner/Specs2Framework.scala
// Specs (org.specs.runner.SpecsFramework) is deprecated; use Specs2.
object Specs2 extends FrameworkDescriptor(
  name = "specs2",
  displayName = "Spec2",
  group = "org.specs2",
  artifact = "specs2-core",
  versionDefault = "5.5.2",
  className = "org.specs2.runner.Specs2Framework",
  sharedPackages = List("org.specs2.runner") // TODO more?
):
  override def args(testTagsFilter: TestTagsFilter): Seq[String] =
    FrameworkDescriptor.listOption("include", testTagsFilter.include) ++
    FrameworkDescriptor.listOption("exclude", testTagsFilter.exclude)
