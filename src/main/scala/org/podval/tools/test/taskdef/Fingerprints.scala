package org.podval.tools.test.taskdef

import sbt.testing.{AnnotatedFingerprint, Fingerprint, SubclassFingerprint}

// I can't rely on the test frameworks implementing `equals()` on `Fingerprint`s correctly.
object Fingerprints extends Ops[Fingerprint](":"):
  def toString(value: Fingerprint): String = value match
    case annotated: AnnotatedFingerprint =>
      s"AnnotatedFingerprint(${annotated.annotationName}, isModule=${annotated.isModule})"
    case subclass: SubclassFingerprint =>
      s"SubclassFingerprint(${subclass.superclassName}, isModule=${subclass.isModule}, requireNoArgConstructor=${subclass.requireNoArgConstructor})"

  override protected def toStrings(value: Fingerprint): Array[String] = value match
    case annotated: AnnotatedFingerprint => Array(
      Ops.toString(true),
      annotated.annotationName,
      Ops.toString(annotated.isModule)
    )
    case subclass: SubclassFingerprint => Array(
      Ops.toString(false),
      subclass.superclassName,
      Ops.toString(subclass.isModule),
      Ops.toString(subclass.requireNoArgConstructor)
    )

  override protected def fromStrings(strings: Array[String]): Fingerprint =
    val isAnnotated: Boolean = Ops.toBoolean(strings(0))
    val name: String = strings(1)
    val isModule: Boolean = Ops.toBoolean(strings(2))
    if isAnnotated
    then AnnotatedFingerprintImpl(
      annotationName = name,
      isModule = isModule
    )
    else SubclassFingerprintImpl(
      superclassName = name,
      isModule = isModule,
      requireNoArgConstructor = Ops.toBoolean(strings(3))
    )
