package org.podval.tools.testing.framework

import org.podval.tools.testing.worker.TestTagsFilter

// https://github.com/sbt/junit-interface
// https://github.com/sbt/junit-interface/blob/develop/src/main/java/com/novocode/junit/JUnitFramework.java
// Note: no nested tasks
// brings in test-interface
object JUnit4 extends FrameworkDescriptor(
  name = "JUnit",
  implementationClassName = "com.novocode.junit.JUnitFramework"
):
  override def args(testTagsFilter: TestTagsFilter): Array[String] = Array.empty
