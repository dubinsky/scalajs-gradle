package org.podval.tools.test.taskdef

import sbt.testing.{Framework, TaskDef}

final class TestClassRunForking(
  override val frameworkName: String,
  taskDef: TaskDef
) extends TestClassRun(
  taskDef
):
  override lazy val framework: Framework = frameworkDescriptor.newInstance.asInstanceOf[Framework]

object TestClassRunForking extends Ops[TestClassRunForking]("@"):
  override protected def toStrings(value: TestClassRunForking): Array[String] = Array(
    value.frameworkName,
    TaskDefs.write(value.taskDef)
  )

  override protected def fromStrings(strings: Array[String]): TestClassRunForking = TestClassRunForking(
    frameworkName = strings(0),
    taskDef = TaskDefs.read(strings(1))
  )
