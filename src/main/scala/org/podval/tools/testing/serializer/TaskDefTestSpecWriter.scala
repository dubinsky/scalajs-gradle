package org.podval.tools.testing.serializer

import org.gradle.api.internal.tasks.testing.TestClassRunInfo
import org.podval.tools.testing.framework.FrameworkDescriptor
import sbt.testing.{Fingerprint, Selector, TaskDef}

// Note: only used when the tests are forked, to transport TaskDefTestSpec to a TestWorker;
// when not forked, the original instance is used.
object TaskDefTestSpecWriter:
  def write(value: TaskDefTestSpec): String =
    val frameworkName: String = value.framework.fold(identity, _.name)
    val fingerprint: String = FingerprintWriter.write(value.taskDef.fingerprint)
    val selectors: String = value.taskDef.selectors.map(SelectorWriter.write).mkString("-")
    s"$frameworkName#${value.taskDef.fullyQualifiedName}#${value.taskDef.explicitlySpecified}#$fingerprint#$selectors"

  def read(testClassRunInfo: TestClassRunInfo): TaskDefTestSpec = testClassRunInfo match
      case taskDefTestSpec: TaskDefTestSpec => taskDefTestSpec
      case _ => read(testClassRunInfo.getTestClassName)

  private def read(string: String): TaskDefTestSpec =
    val parts: Array[String] = string.split("#")
    val frameworkName: String = parts(0)
    val fullyQualifiedName: String = parts(1)
    val explicitlySpecified: Boolean = parts(2).toBoolean
    val fingerprint: Fingerprint = FingerprintWriter.read(parts(3))
    val selectors: Array[Selector] = parts(4).split("-").map(SelectorWriter.read)

    TaskDefTestSpec(
      framework = Left(frameworkName),
      taskDef = TaskDef(
        fullyQualifiedName,
        fingerprint,
        explicitlySpecified,
        selectors
      )
    )
