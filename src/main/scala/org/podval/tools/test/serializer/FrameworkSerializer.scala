package org.podval.tools.test.serializer

import org.gradle.internal.serialize.{Decoder, Encoder, Serializer}
import org.podval.tools.test.framework.FrameworkDescriptor
import sbt.testing.{Fingerprint, Framework}

final class FrameworkSerializer extends Serializer[Framework]:
  override def write(encoder: Encoder, value: Framework): Unit =
    encoder.writeString(FrameworkDescriptor.forFramework(value).implementationClassName)

  override def read(decoder: Decoder): Framework =
    FrameworkSerializer.instantiate(decoder.readString)

object FrameworkSerializer:
  def instantiate(className: String): Framework =
    Class.forName(className).getConstructor().newInstance().asInstanceOf[Framework]

  def equal(left: Framework, right: Framework): Boolean =
    left.name == right.name &&
    left.fingerprints.length == right.fingerprints.length &&
    left.fingerprints.zip(right.fingerprints).forall((left: Fingerprint, right: Fingerprint) => FingerprintSerializer.equal(left, right))
