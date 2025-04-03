package org.podval.tools.test.taskdef

import sbt.testing.{AnnotatedFingerprint, Fingerprint, SubclassFingerprint}

object Fingerprints:
  // I can't rely on the test frameworks implementing `equals()` on `Fingerprint`s correctly.
  def equal(left: Fingerprint, right: Fingerprint): Boolean = (left, right) match
    case (left: AnnotatedFingerprint, right: AnnotatedFingerprint) =>
      left.annotationName == right.annotationName &&
      left.isModule == right.isModule
    case (left: SubclassFingerprint, right: SubclassFingerprint) =>
      left.superclassName == right.superclassName &&
      left.isModule == right.isModule &&
      left.requireNoArgConstructor == right.requireNoArgConstructor
    case _ => false  
  
  def toString(value: Fingerprint): String = value match
    case annotated: AnnotatedFingerprint =>
      s"AnnotatedFingerprint(${annotated.annotationName}, isModule=${annotated.isModule})"
    case subclass: SubclassFingerprint =>
      s"SubclassFingerprint(${subclass.superclassName}, isModule=${subclass.isModule}, requireNoArgConstructor=${subclass.requireNoArgConstructor})"
