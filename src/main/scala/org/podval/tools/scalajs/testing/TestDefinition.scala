package org.podval.tools.scalajs.testing

import sbt.testing.{Fingerprint, Selector}

// Note: based on sbt.TestFramework from org.scala-sbt.testing
final class TestDefinition(
  val name: String,
  val fingerprint: Fingerprint,
  val explicitlySpecified: Boolean,
  val selectors: Array[Selector]
):
  override def toString: String = "Test " + name + " : " + TestFramework.toString(fingerprint)

  override def equals(t: Any): Boolean = t.asInstanceOf[Matchable] match
    case r: TestDefinition => name == r.name && TestFramework.matches(fingerprint, r.fingerprint)
    case _                 => false

  override def hashCode: Int = (name.hashCode, TestFramework.hashCode(fingerprint)).hashCode
