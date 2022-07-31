package org.podval.tools.test.serializer

import org.gradle.internal.serialize.{Decoder, Encoder, Serializer}
import sbt.testing.{AnnotatedFingerprint, Fingerprint, SubclassFingerprint}

final class FingerprintSerializer extends Serializer[Fingerprint]:
  override def write(encoder: Encoder, value: Fingerprint): Unit = value match
    case annotated: AnnotatedFingerprint =>
      encoder.writeBoolean(true)
      encoder.writeString (annotated.annotationName)
      encoder.writeBoolean(annotated.isModule)
    case subclass: SubclassFingerprint =>
      encoder.writeBoolean(false)
      encoder.writeString (subclass.superclassName)
      encoder.writeBoolean(subclass.isModule)
      encoder.writeBoolean(subclass.requireNoArgConstructor)

  override def read(decoder: Decoder): Fingerprint =
    val isAnnotated: Boolean = decoder.readBoolean
    if isAnnotated
    then
      AnnotatedFingerprintImpl(
        annotationName = decoder.readString,
        isModule = decoder.readBoolean
      )
    else
      SubclassFingerprintImpl(
        superclassName = decoder.readString,
        isModule = decoder.readBoolean,
        requireNoArgConstructor = decoder.readBoolean
      )

object FingerprintSerializer:
  def equal(left: Fingerprint, right: Fingerprint): Boolean = (left, right) match
    case (left: AnnotatedFingerprint, right: AnnotatedFingerprint) =>
      left.annotationName == right.annotationName &&
      left.isModule == right.isModule
    case (left: SubclassFingerprint, right: SubclassFingerprint) =>
      left.superclassName == right.superclassName &&
      left.isModule == right.isModule &&
      left.requireNoArgConstructor == right.requireNoArgConstructor
    case _ => false

