package org.podval.tools.test.serializer

import org.gradle.internal.serialize.{Decoder, Encoder, Serializer}
import sbt.testing.{Fingerprint, Selector, TaskDef}

final class TaskDefSerializer(
  fingerprintSerializer: FingerprintSerializer,
  selectorsSerializer: ArraySerializer[Selector]
) extends Serializer[TaskDef]:

  override def write(encoder: Encoder, value: TaskDef): Unit =
    encoder.writeString(value.fullyQualifiedName)
    fingerprintSerializer.write(encoder, value.fingerprint)
    encoder.writeBoolean(value.explicitlySpecified)
    selectorsSerializer.write(encoder, value.selectors)

  override def read(decoder: Decoder): TaskDef =
    val fullyQualifiedName: String = decoder.readString
    val fingerprint: Fingerprint = fingerprintSerializer.read(decoder)
    val explicitlySpecified: Boolean = decoder.readBoolean
    val selectors: Array[Selector] = selectorsSerializer.read(decoder)

    TaskDef(
      fullyQualifiedName,
      fingerprint,
      explicitlySpecified,
      selectors
    )

object TaskDefSerializer:
  private given CanEqual[Selector, Selector] = CanEqual.derived

  def equal(left: TaskDef, right: TaskDef): Boolean =
    left.fullyQualifiedName == right.fullyQualifiedName &&
    FingerprintSerializer.equal(left.fingerprint, right.fingerprint) &&
    left.explicitlySpecified == right.explicitlySpecified &&
    left.selectors.length == right.selectors.length &&
    left.selectors.zip(right.selectors).forall((left: Selector, right: Selector) => SelectorSerializer.equal(left, right))
