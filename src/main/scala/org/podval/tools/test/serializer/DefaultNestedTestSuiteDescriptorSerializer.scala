package org.podval.tools.test.serializer

import org.gradle.api.internal.tasks.testing.DefaultNestedTestSuiteDescriptor
import org.gradle.internal.id.CompositeIdGenerator.CompositeId
import org.gradle.internal.serialize.{Decoder, Encoder, Serializer}

final class DefaultNestedTestSuiteDescriptorSerializer extends Serializer[DefaultNestedTestSuiteDescriptor]:
  private val idSerializer: CompositeIdSerializer = new CompositeIdSerializer

  override def read(decoder: Decoder): DefaultNestedTestSuiteDescriptor =
    val id: CompositeId = idSerializer.read(decoder)
    val name: String = decoder.readString
    val displayName: String = decoder.readString
    val parentId: Object = idSerializer.read(decoder)

    new DefaultNestedTestSuiteDescriptor(
      id,
      name,
      displayName,
      parentId.asInstanceOf[CompositeId]
    )

  override def write(encoder: Encoder, value: DefaultNestedTestSuiteDescriptor): Unit =
    idSerializer.write(encoder, value.getId.asInstanceOf[CompositeId])
    encoder.writeString(value.getName)
    encoder.writeString(value.getDisplayName)
    idSerializer.write(encoder, value.getParentId)
