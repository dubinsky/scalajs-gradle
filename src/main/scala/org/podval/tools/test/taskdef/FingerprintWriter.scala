package org.podval.tools.test.taskdef

import sbt.testing.{AnnotatedFingerprint, Fingerprint, SubclassFingerprint}

object FingerprintWriter:
  def write(value: Fingerprint): String = value match
    case annotated: AnnotatedFingerprint => s"true:${annotated.annotationName}:${annotated.isModule}"
    case subclass : SubclassFingerprint  => s"false:${subclass.superclassName}:${subclass .isModule}:${subclass.requireNoArgConstructor}"

  def read(string: String): Fingerprint =
    val parts: Array[String] = string.split(":")
    val isAnnotated: Boolean = parts(0) == "true"
    val name: String = parts(1)
    val isModule: Boolean = parts(2) == "true"
    if isAnnotated
    then AnnotatedFingerprintImpl(annotationName = name, isModule = isModule)
    else SubclassFingerprintImpl (superclassName = name, isModule = isModule, requireNoArgConstructor = parts(3) == "true")
  