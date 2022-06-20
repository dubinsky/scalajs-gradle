package org.podval.tools.scalajs.testing

import sbt.testing.{Fingerprint, Selector}

// Note: based on sbt.TestFramework from org.scala-sbt.testing
final class TestDefinition(
  val name: String,
  val fingerprint: Fingerprint,
  val explicitlySpecified: Boolean,
  val selectors: Array[Selector]
):
  override def toString: String = s"Test $name : ${Util.toString(fingerprint)}"

  override def equals(t: Any): Boolean = t.asInstanceOf[Matchable] match
    case r: TestDefinition => name == r.name && Util.matches(fingerprint, r.fingerprint)
    case _                 => false

  override def hashCode: Int = (name.hashCode, Util.hashCode(fingerprint)).hashCode
