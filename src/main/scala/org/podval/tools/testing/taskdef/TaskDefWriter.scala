package org.podval.tools.testing.taskdef

import sbt.testing.{Fingerprint, Selector, TaskDef}

object TaskDefWriter:
  def write(value: TaskDef): String =
    val fingerprint: String = FingerprintWriter.write(value.fingerprint)
    val selectors: String = value.selectors.map(SelectorWriter.write).mkString("-")
    s"${value.fullyQualifiedName}#${value.explicitlySpecified}#$fingerprint#$selectors"

  def read(string: String): TaskDef =
    val parts: Array[String] = string.split("#")
    val fullyQualifiedName: String = parts(0)
    val explicitlySpecified: Boolean = parts(1).toBoolean
    val fingerprint: Fingerprint = FingerprintWriter.read(parts(2))
    val selectors: Array[Selector] = parts(3).split("-").map(SelectorWriter.read)

    TaskDef(
      fullyQualifiedName,
      fingerprint,
      explicitlySpecified,
      selectors
    )
