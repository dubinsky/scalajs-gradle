package org.podval.tools.testing.framework

import org.podval.tools.testing.worker.TestTagsFilter

// https://github.com/sbt/junit-interface
// https://github.com/sbt/junit-interface/blob/develop/src/main/java/com/novocode/junit/JUnitFramework.java
// Note: no nested tasks
object JUnit4 extends FrameworkDescriptor(
  name = "JUnit",
  displayName = "JUnit4",
  group = "com.github.sbt",
  artifact = "junit-interface",
  versionDefault = "0.13.3",
  className = "com.novocode.junit.JUnitFramework",
  sharedPackages = List("com.novocode.junit", "junit.framework", "junit.extensions", "org.junit")
):
  override def isScalaJSSupported: Boolean = false
  override def isScalaDependency: Boolean = false

  override def args(testTagsFilter: TestTagsFilter): Seq[String] =
    FrameworkDescriptor.listOption("--include-categories", testTagsFilter.include) ++
    FrameworkDescriptor.listOption("--exclude-categories", testTagsFilter.exclude)
