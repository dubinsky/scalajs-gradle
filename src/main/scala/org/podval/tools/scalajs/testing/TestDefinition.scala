package org.podval.tools.scalajs.testing

import sbt.testing.{AnnotatedFingerprint, Fingerprint, SubclassFingerprint, TaskDef}

// TODO dissolve into various TestDescriptors
// Note: based on sbt.TestFramework from org.scala-sbt.testing
final class TestDefinition(val taskDef: TaskDef):
  def name: String = taskDef.fullyQualifiedName

  override def toString: String =
    s"""
       |TestDefinition(
       |  name=$name,
       |  fingerprint=${Util.toString(taskDef.fingerprint)},
       |  explicitlySpecified=${taskDef.explicitlySpecified},
       |  selectors=${taskDef.selectors.toList}
       |)
       |""".stripMargin

  override def equals(t: Any): Boolean = t.asInstanceOf[Matchable] match
    case r: TestDefinition => name == r.name && TestDefinition.matches(taskDef.fingerprint, r.taskDef.fingerprint)
    case _                 => false

  override def hashCode: Int = (name.hashCode, TestDefinition.hashCode(taskDef.fingerprint)).hashCode

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
