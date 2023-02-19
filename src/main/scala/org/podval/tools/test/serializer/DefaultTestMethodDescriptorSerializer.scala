package org.podval.tools.test.serializer

import org.gradle.api.internal.tasks.testing.DefaultTestMethodDescriptor
import org.gradle.internal.id.CompositeIdGenerator.CompositeId
import org.gradle.internal.serialize.{Decoder, Encoder, Serializer}

final class DefaultTestMethodDescriptorSerializer extends Serializer[DefaultTestMethodDescriptor]:
  private val idSerializer: CompositeIdSerializer = new CompositeIdSerializer
  
  override def read(decoder: Decoder): DefaultTestMethodDescriptor =
    val id: CompositeId = idSerializer.read(decoder)
    val className: String = decoder.readString
    val name: String = decoder.readString

    new DefaultTestMethodDescriptor(
      id,
      className,
      name
    )

  override def write(encoder: Encoder, value: DefaultTestMethodDescriptor): Unit =
    idSerializer.write(encoder, value.getId.asInstanceOf[CompositeId])
    encoder.writeString(value.getClassName)
    encoder.writeString(value.getName)
