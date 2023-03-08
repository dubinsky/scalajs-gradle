package org.podval.tools.testing.serializer

import org.gradle.api.internal.tasks.testing.TestClassRunInfo
import sbt.testing.{Framework, TaskDef}

// This is used to convey the data about the test to the TestClassProcessor.
// For forked Scala tests, only the Framework name gets serialized and is instantiated as needed;
// *but* for ScalaJS Framework is retrieved from the Node side and can not be instantiated...
final class TaskDefTestSpec(
  val framework: Either[String, Framework],
  val taskDef: TaskDef
) extends TestClassRunInfo:
  override def getTestClassName: String = taskDef.fullyQualifiedName
