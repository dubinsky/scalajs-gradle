package org.podval.tools.test.framework

import org.podval.tools.test.TestTagging

// https://github.com/sbt/junit-interface
// https://github.com/sbt/junit-interface/blob/develop/src/main/java/com/novocode/junit/JUnitFramework.java
// Note: no nested tasks
object JUnit4 extends FrameworkDescriptor(
  name = "JUnit",
  implementationClassName = "com.novocode.junit.JUnitFramework"
):
  override def args(testTagging: TestTagging): Array[String] = Array.empty
