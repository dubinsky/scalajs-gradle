package org.podval.tools.testing.framework

import org.podval.tools.testing.worker.TestTagsFilter

// https://github.com/sbt/sbt-jupiter-interface
// https://github.com/sbt/sbt-jupiter-interface/blob/master/src/library/src/main/java/net/aichler/jupiter/api/JupiterFramework.java
// brings in test-interface
object JUnit5 extends FrameworkDescriptor(
  name = "Jupiter",
  implementationClassName = "net.aichler.jupiter.api.JupiterFramework"
):
  override def args(testTagsFilter: TestTagsFilter): Seq[String] = Seq.empty
