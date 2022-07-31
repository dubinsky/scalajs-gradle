package org.podval.tools.test.serializer

import org.gradle.internal.serialize.{Decoder, Encoder, Serializer}
import scala.reflect.ClassTag

final class ArraySerializer[T: ClassTag](
  elementSerializer: Serializer[T]
) extends Serializer[Array[T]]:

  override def write(encoder: Encoder, value: Array[T]): Unit =
    encoder.writeInt(value.length)
    for element: T <- value do elementSerializer.write(encoder, element)

  override def read(decoder: Decoder): Array[T] =
    val length: Int = decoder.readInt
    for _ <- 0.until(length).toArray yield elementSerializer.read(decoder)
