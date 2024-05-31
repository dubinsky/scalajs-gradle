package org.podval.tools.testing.framework

import org.podval.tools.testing.worker.TestTagsFilter

// https://scalameta.org/munit/
// https://github.com/scalameta/munit/blob/main/munit/jvm/src/main/scala/munit/Framework.scala
// https://github.com/scalameta/munit/blob/main/munit/non-jvm/src/main/scala/munit/Framework.scala
// https://github.com/scalameta/munit/blob/main/junit-interface/src/main/java/munit/internal/junitinterface/JUnitFramework.java
object MUnit extends FrameworkDescriptor(
  name = "munit",
  displayName = "MUnit",
  group = "org.scalameta",
  artifact = "munit",
  versionDefault = "1.0.0",
  className = "munit.Framework",
  sharedPackages = List("munit")
):
  override def args(testTagsFilter: TestTagsFilter): Seq[String] = Seq.empty
