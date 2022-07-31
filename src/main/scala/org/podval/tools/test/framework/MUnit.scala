package org.podval.tools.test.framework

import org.podval.tools.test.TestTagging

// https://scalameta.org/munit/
// https://github.com/scalameta/munit/blob/main/munit/jvm/src/main/scala/munit/Framework.scala
// https://github.com/scalameta/munit/blob/main/munit/non-jvm/src/main/scala/munit/Framework.scala
// https://github.com/scalameta/munit/blob/main/junit-interface/src/main/java/munit/internal/junitinterface/JUnitFramework.java
object MUnit extends FrameworkDescriptor(
  name = "munit",
  implementationClassName = "munit.Framework"
):
  override def args(testTagging: TestTagging): Array[String] = Array.empty

