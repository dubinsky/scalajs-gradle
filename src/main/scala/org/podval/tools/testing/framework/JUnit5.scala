package org.podval.tools.testing.framework

import org.podval.tools.build.{JavaDependency, Version}
import org.podval.tools.testing.worker.TestTagsFilter

// https://github.com/sbt/sbt-jupiter-interface
// https://github.com/sbt/sbt-jupiter-interface/blob/master/src/library/src/main/java/net/aichler/jupiter/api/JupiterFramework.java
object JUnit5 extends FrameworkDescriptor(
  name = "Jupiter",
  displayName = "JUnit5",
  group = null,
  artifact = null,
  versionDefault = null,
  className = "net.aichler.jupiter.api.JupiterFramework",
  sharedPackages = List("net.aichler.jupiter.api", "org.junit")
) with JavaDependency.Maker:
  override def isJvmSupported: Boolean = false
  override def isScalaJSSupported: Boolean = false

  override def args(testTagsFilter: TestTagsFilter): Seq[String] = Seq.empty
