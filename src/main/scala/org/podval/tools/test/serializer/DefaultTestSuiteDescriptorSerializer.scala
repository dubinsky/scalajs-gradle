package org.podval.tools.test.serializer

import org.gradle.api.internal.tasks.testing.DefaultTestSuiteDescriptor
import org.gradle.internal.id.CompositeIdGenerator.CompositeId
import org.gradle.internal.serialize.{Decoder, Encoder, Serializer}

final class DefaultTestSuiteDescriptorSerializer extends Serializer[DefaultTestSuiteDescriptor]:
  private val idSerializer: CompositeIdSerializer = new CompositeIdSerializer

  override def read(decoder: Decoder): DefaultTestSuiteDescriptor =
    val id: CompositeId = idSerializer.read(decoder)
    val name: String = decoder.readString
    new DefaultTestSuiteDescriptor(
      id,
      name
    )

  override def write(encoder: Encoder, value: DefaultTestSuiteDescriptor): Unit =
    idSerializer.write(encoder, value.getId.asInstanceOf[CompositeId])
    encoder.writeString(value.getName)
