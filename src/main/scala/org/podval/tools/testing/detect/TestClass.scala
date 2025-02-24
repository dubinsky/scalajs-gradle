package org.podval.tools.testing.detect

import sbt.testing.{Framework, TaskDef}

class TestClass(
  val sourceFilePath: String,
  val classFilePath: String,
  val framework: Framework,
  val taskDef: TaskDef
)
