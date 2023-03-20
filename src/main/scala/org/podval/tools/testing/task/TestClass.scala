package org.podval.tools.testing.task

import sbt.testing.{Framework, TaskDef}

class TestClass(
  val sourceFilePath: String,
  val classFilePath: String,
  val framework: Framework,
  val taskDef: TaskDef
)
