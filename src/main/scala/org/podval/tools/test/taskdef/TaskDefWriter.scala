package org.podval.tools.test.taskdef

import org.podval.tools.util.Scala212Collections.arrayMap
import sbt.testing.{Fingerprint, Selector, TaskDef}

object TaskDefWriter:
  def write(value: TaskDef): String =
    val fingerprint: String = FingerprintWriter.write(value.fingerprint)
    val selectors: String = value.selectors.map(SelectorWriter.write).mkString("-")
    s"${value.fullyQualifiedName}#${value.explicitlySpecified}#$fingerprint#$selectors"

  def read(string: String): TaskDef =
    val parts: Array[String] = string.split("#")
    val fullyQualifiedName: String = parts(0)
    val explicitlySpecified: Boolean = parts(1) == "true"
    val fingerprint: Fingerprint = FingerprintWriter.read(parts(2))
    val selectors: Array[Selector] = arrayMap(parts(3).split("-"), SelectorWriter.read)
      
    TaskDef(
      fullyQualifiedName,
      fingerprint,
      explicitlySpecified,
      selectors
    )
