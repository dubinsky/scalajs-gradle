package org.podval.tools.test.taskdef

import sbt.testing.{AnnotatedFingerprint, Fingerprint, SubclassFingerprint}

object Fingerprints:
  // Note: I can't rely on all the frameworks providing equals() on their Fingerprint implementations...
  def equal(left: Fingerprint, right: Fingerprint): Boolean = (left, right) match
    case (left: AnnotatedFingerprint, right: AnnotatedFingerprint) =>
      left.annotationName == right.annotationName &&
      left.isModule == right.isModule
    case (left: SubclassFingerprint, right: SubclassFingerprint) =>
      left.superclassName == right.superclassName &&
      left.isModule == right.isModule &&
      left.requireNoArgConstructor == right.requireNoArgConstructor
    case _ => false
    
  def write(value: Fingerprint): String = value match
    case annotated: AnnotatedFingerprint => s"true:${annotated.annotationName}:${annotated.isModule}"
    case subclass: SubclassFingerprint => s"false:${subclass.superclassName}:${subclass.isModule}:${subclass.requireNoArgConstructor}"

  def read(string: String): Fingerprint =
    val parts: Array[String] = string.split(":")
    val isAnnotated: Boolean = parts(0) == "true"
    val name: String = parts(1)
    val isModule: Boolean = parts(2) == "true"
    if isAnnotated
    then AnnotatedFingerprintImpl(annotationName = name, isModule = isModule)
    else SubclassFingerprintImpl (superclassName = name, isModule = isModule, requireNoArgConstructor = parts(3) == "true")
