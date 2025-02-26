package org.podval.tools.test.taskdef

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
    val selectorStrings: Array[String] = parts(3).split("-")
    // Using `val selectors: Array[Selector] = selectorStrings.map(SelectorWriter.read)` here results in
    // java.lang.NoSuchMethodError: 'java.lang.Object scala.Predef$.refArrayOps(java.lang.Object[])
    // on Scala 2.12.
    // TODO this can probably be less ugly...
    val selectors: Array[Selector] = new Array[Selector](selectorStrings.length)
    var i = 0
    while i < selectorStrings.length do
      selectors(i) = SelectorWriter.read(selectorStrings(i))
      i = i + 1

    TaskDef(
      fullyQualifiedName,
      fingerprint,
      explicitlySpecified,
      selectors
    )
