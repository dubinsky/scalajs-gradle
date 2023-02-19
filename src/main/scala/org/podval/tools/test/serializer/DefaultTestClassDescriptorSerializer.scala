package org.podval.tools.test.serializer

import org.gradle.api.internal.tasks.testing.DefaultTestClassDescriptor
import org.gradle.internal.id.CompositeIdGenerator.CompositeId
import org.gradle.internal.serialize.{Decoder, Encoder, Serializer}

final class DefaultTestClassDescriptorSerializer extends Serializer[DefaultTestClassDescriptor]:
  private val idSerializer: CompositeIdSerializer = new CompositeIdSerializer

  override def read(decoder: Decoder): DefaultTestClassDescriptor =
    val id: CompositeId = idSerializer.read(decoder)
    val name: String = decoder.readString
    val displayName: String = decoder.readString

    new DefaultTestClassDescriptor(
      id,
      name,
      displayName
    )

  override def write(encoder: Encoder, value: DefaultTestClassDescriptor): Unit =
    idSerializer.write(encoder, value.getId.asInstanceOf[CompositeId])
    encoder.writeString(value.getName)
    encoder.writeString(value.getDisplayName)
