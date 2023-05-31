package org.podval.tools.testing.framework

import org.podval.tools.testing.worker.TestTagsFilter

// implementation: https://github.com/scalatest/scalatest/blob/main/jvm/core/src/main/scala/org/scalatest/tools/Framework.scala
// runner arguments: https://www.scalatest.org/user_guide/using_the_runner
// Note: no nested tasks
// DOES NOT bring in test-interface (in non-ScalaJS variant)!
object ScalaTest extends FrameworkDescriptor(
  name = "ScalaTest",
  displayName = "ScalaTest",
  group = "org.scalatest",
  artifact = "scalatest",
  versionDefault = "3.2.16",
  className = "org.scalatest.tools.Framework",
  sharedPackages = List("org.scalatest.tools")  // TODO more?
):
  override def args(
    testTagsFilter: TestTagsFilter
  ): Seq[String] =
    FrameworkDescriptor.listOfOptions("-n", testTagsFilter.include) ++
    FrameworkDescriptor.listOfOptions("-l", testTagsFilter.exclude)
