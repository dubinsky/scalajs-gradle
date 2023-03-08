package org.podval.tools.testing.task

import sbt.testing.{Framework, TaskDef}

class TestClass(
  val sourceFilePath: String,  // TODO remove?
  val classFilePath: String,
  val framework: Framework,
  val taskDef: TaskDef
)
