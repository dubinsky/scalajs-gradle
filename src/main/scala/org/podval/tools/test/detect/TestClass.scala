package org.podval.tools.test.detect

import sbt.testing.{Framework, TaskDef}

class TestClass(
  val sourceFilePath: String,
  val classFilePath: String,
  val framework: Framework,
  val taskDef: TaskDef
)
