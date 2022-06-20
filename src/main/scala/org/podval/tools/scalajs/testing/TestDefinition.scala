package org.podval.tools.scalajs.testing

import sbt.testing.{AnnotatedFingerprint, Fingerprint, Selector, SubclassFingerprint}

// Note: based on sbt.TestFramework from org.scala-sbt.testing
final class TestDefinition(
  val name: String,
  val fingerprint: Fingerprint,
  val explicitlySpecified: Boolean,
  val selectors: Array[Selector]
):
  override def toString: String = s"Test $name : ${TestDefinition.toString(fingerprint)}"

  override def equals(t: Any): Boolean = t.asInstanceOf[Matchable] match
    case r: TestDefinition => name == r.name && TestDefinition.matches(fingerprint, r.fingerprint)
    case _                 => false

  override def hashCode: Int = (name.hashCode, TestDefinition.hashCode(fingerprint)).hashCode

object TestDefinition:
  def hashCode(f: Fingerprint): Int = f match
    case s: SubclassFingerprint  => (s.isModule, s.superclassName).hashCode
    case a: AnnotatedFingerprint => (a.isModule, a.annotationName).hashCode
    case _                       => 0

  def matches(a: Fingerprint, b: Fingerprint): Boolean = (a, b) match
    case (a: SubclassFingerprint, b: SubclassFingerprint) =>
      a.isModule == b.isModule && a.superclassName == b.superclassName
    case (a: AnnotatedFingerprint, b: AnnotatedFingerprint) =>
      a.isModule == b.isModule && a.annotationName == b.annotationName
    case _ => false

  def toString(f: Fingerprint): String = f match
    case sf: SubclassFingerprint  => s"subclass(${sf.isModule}  , ${sf.superclassName})"
    case af: AnnotatedFingerprint => s"annotation(${af.isModule}, ${af.annotationName})"
    case _                        => f.toString
