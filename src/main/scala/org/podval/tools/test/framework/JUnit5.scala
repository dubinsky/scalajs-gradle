package org.podval.tools.test.framework

import org.podval.tools.test.TestTagsFilter

// https://github.com/sbt/sbt-jupiter-interface
// https://github.com/sbt/sbt-jupiter-interface/blob/master/src/library/src/main/java/net/aichler/jupiter/api/JupiterFramework.java
object JUnit5 extends FrameworkDescriptor(
  name = "Jupiter",
  implementationClassName = "net.aichler.jupiter.api.JupiterFramework"
):
  override def args(testTagsFilter: TestTagsFilter): Array[String] = Array.empty
