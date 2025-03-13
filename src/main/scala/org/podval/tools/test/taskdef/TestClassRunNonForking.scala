package org.podval.tools.test.taskdef

import sbt.testing.{Framework, TaskDef}

final class TestClassRunNonForking(
  override val framework: Framework,
  taskDef: TaskDef
) extends TestClassRun(
  taskDef
):
  override def frameworkName: String = framework.name

  override def toString: String = TaskDefs.toString(taskDef)
