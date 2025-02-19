package org.podval.tools.testing.framework

import org.podval.tools.build.{JavaDependency, Version}
import org.podval.tools.testing.worker.TestTagsFilter

// https://github.com/sbt/sbt-jupiter-interface
// https://github.com/sbt/sbt-jupiter-interface/blob/master/src/library/src/main/java/net/aichler/jupiter/api/JupiterFramework.java
object JUnit5 extends FrameworkDescriptor(
  name = "Jupiter",
  displayName = "JUnit5",
  group = "com.github.sbt.junit",
  artifact = "jupiter-interface",
  versionDefault = Version("0.13.3"),
  className = "com.github.sbt.junit.jupiter.api.JupiterFramework",
  sharedPackages = List("com.github.sbt.junit.jupiter.api", "org.junit")
) with JavaDependency.Maker:
  // This is a JVM-only test framework
  override protected def isScalaJSSupported: Boolean = false

  // TODO https://github.com/dubinsky/scalajs-gradle/issues/38
  override protected def isJvmSupported: Boolean = false

  override def args(testTagsFilter: TestTagsFilter): Seq[String] =
    FrameworkDescriptor.listOption("--include-tags", testTagsFilter.include) ++
    FrameworkDescriptor.listOption("--exclude-tags", testTagsFilter.exclude)
