package org.podval.tools.test.detect

import sbt.testing.{Framework, TaskDef}

class TestClass(
  val sourceFilePath: String,
  val classFilePath: String,
  val framework: Framework,
  val taskDef: TaskDef
):
  def set(matches: TestFilterMatch): TestClass = TestClass(
    sourceFilePath = sourceFilePath,
    classFilePath = classFilePath,
    framework = framework,
    taskDef = TaskDef(
      taskDef.fullyQualifiedName,
      taskDef.fingerprint,
      matches.explicitlySpecified,
      matches.selectors
    )
  )
