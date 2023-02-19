package org.podval.tools.test.serializer

import org.gradle.internal.id.CompositeIdGenerator.CompositeId
import org.gradle.internal.serialize.{Decoder, Encoder, Serializer}
import scala.annotation.tailrec

final class CompositeIdSerializer extends Serializer[CompositeId]:
  override def read(decoder: Decoder): CompositeId =
    @tailrec
    def create(longs: List[Long], result: CompositeId): CompositeId = longs match
      case Nil => result
      case long::tail => create(tail, CompositeId(result, long))

    val length: Int = decoder.readSmallInt()
    0.until(length).map(_ => decoder.readLong()).toList match
      case first::second::longs => create(longs, CompositeId(first, second))

  override def write(encoder: Encoder, value: CompositeId): Unit =
    @tailrec
    def get(id: CompositeId, result: List[Long]): List[Long] =
      val long: Long = id.getId.asInstanceOf[Long]
      id.getScope match
        case simple: Long => simple +: long +: result  // TODO Unreachable case?!
        case composite: CompositeId => get(composite, long +: result)

    val longs: List[Long] = get(value, List.empty)
    encoder.writeSmallInt(longs.length)
    for long: Long <- longs do encoder.writeLong(long)

