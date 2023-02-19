package org.podval.tools.test.serializer

import org.gradle.api.internal.tasks.testing.DefaultTestDescriptor
import org.gradle.internal.id.CompositeIdGenerator.CompositeId
import org.gradle.internal.serialize.{Decoder, Encoder, Serializer}

final class DefaultTestDescriptorSerializer extends Serializer[DefaultTestDescriptor]:
  private val idSerializer: CompositeIdSerializer = new CompositeIdSerializer
  
  override def read(decoder: Decoder): DefaultTestDescriptor =
    val id: CompositeId = idSerializer.read(decoder)
    val className: String  = decoder.readString()
    val classDisplayName: String = decoder.readString()
    val name: String = decoder.readString()
    val displayName: String = decoder.readString()

    new DefaultTestDescriptor(
      id,
      className,
      name,
      classDisplayName,
      displayName
    )

  override def write(encoder: Encoder, value: DefaultTestDescriptor): Unit =
    idSerializer.write(encoder, value.getId.asInstanceOf[CompositeId])
    encoder.writeString(value.getClassName)
    encoder.writeString(value.getClassDisplayName)
    encoder.writeString(value.getName)
    encoder.writeString(value.getDisplayName)
