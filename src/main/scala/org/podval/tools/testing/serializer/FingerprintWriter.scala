package org.podval.tools.testing.serializer

import sbt.testing.{AnnotatedFingerprint, Fingerprint, SubclassFingerprint}

object FingerprintWriter:
  def write(value: Fingerprint): String = value match
    case annotated: AnnotatedFingerprint => s"true:${annotated.annotationName}:${annotated.isModule}"
    case subclass : SubclassFingerprint  => s"false:${subclass.superclassName}:${subclass .isModule}:${subclass.requireNoArgConstructor}"

  def read(string: String): Fingerprint =
    val parts: Array[String] = string.split(':')
    val isAnnotated: Boolean = parts(0).toBoolean
    if isAnnotated
    then AnnotatedFingerprintImpl(annotationName = parts(1), isModule = parts(2).toBoolean)
    else SubclassFingerprintImpl (superclassName = parts(1), isModule = parts(2).toBoolean, requireNoArgConstructor = parts(3).toBoolean)
  