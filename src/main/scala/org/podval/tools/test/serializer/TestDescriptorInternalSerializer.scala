package org.podval.tools.test.serializer

import org.gradle.api.internal.tasks.testing.{DefaultTestDescriptor, TestDescriptorInternal}
import org.gradle.internal.serialize.{Decoder, Encoder, Serializer}

final class TestDescriptorInternalSerializer(
  idSerializer: IdSerializer
) extends Serializer[TestDescriptorInternal]:

  override def write(encoder: Encoder, value: TestDescriptorInternal): Unit =
    idSerializer.write(encoder, value.getId)
    encoder.writeString(value.getClassName)
    encoder.writeString(value.getClassDisplayName)
    encoder.writeString(value.getName)
    encoder.writeString(value.getDisplayName)

  override def read(decoder: Decoder): TestDescriptorInternal =
    val id: Object = idSerializer.read(decoder)
    val className: String = decoder.readString
    val classDisplayName: String = decoder.readString
    val name: String = decoder.readString
    val displayName: String = decoder.readString

    new DefaultTestDescriptor(
      id,
      className,
      name,
      classDisplayName,
      displayName
    )
