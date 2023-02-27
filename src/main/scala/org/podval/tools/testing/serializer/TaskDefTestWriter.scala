package org.podval.tools.testing.serializer

import org.podval.tools.testing.framework.FrameworkDescriptor
import org.podval.tools.testing.worker.TaskDefTest
import sbt.testing.{Fingerprint, Selector, TaskDef}

// Note: only used when the tests are forked, to transport TaskDefTest to a TestWorker;
// when not forked, the original instance is used.
object TaskDefTestWriter:
  def write(value: TaskDefTest): String =
    val frameworkName: String = value.framework.fold(identity, _.name)
    val fingerprint: String = FingerprintWriter.write(value.taskDef.fingerprint)
    val selectors: String = value.taskDef.selectors.map(SelectorWriter.write).mkString("-")
    s"$frameworkName#${value.taskDef.fullyQualifiedName}#${value.taskDef.explicitlySpecified}#$fingerprint#$selectors"

  def read(string: String): TaskDefTest =
    val parts: Array[String] = string.split("#")
    val frameworkName: String = parts(0)
    val fullyQualifiedName: String = parts(1)
    val explicitlySpecified: Boolean = parts(2).toBoolean
    val fingerprint: Fingerprint = FingerprintWriter.read(parts(3))
    val selectors: Array[Selector] = parts(4).split("-").map(SelectorWriter.read)

    TaskDefTest(
      id = null,
      framework = Left(frameworkName),
      taskDef = TaskDef(
        fullyQualifiedName,
        fingerprint,
        explicitlySpecified,
        selectors
      )
    )
