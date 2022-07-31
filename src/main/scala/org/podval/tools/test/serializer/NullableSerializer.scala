package org.podval.tools.test.serializer

import org.gradle.internal.serialize.{Decoder, Encoder, Serializer}

final class NullableSerializer[T](serializer: Serializer[T]) extends Serializer[T] :
  override def write(encoder: Encoder, value: T): Unit =
    val isValuePresent: Boolean = Option(value).isDefined
    encoder.writeBoolean(isValuePresent)
    if isValuePresent then serializer.write(encoder, value)

  override def read(decoder: Decoder): T =
    val isValuePresent: Boolean = decoder.readBoolean
    if !isValuePresent then null.asInstanceOf[T] else serializer.read(decoder)
    