package org.podval.tools.test.serializer

import org.gradle.api.internal.tasks.testing.TestStartEvent
import org.gradle.internal.serialize.{Decoder, Encoder, Serializer}

final class TestStartEventSerializer(
  nullableIdSerializer: NullableSerializer[Object]
) extends Serializer[TestStartEvent]:
  
  override def write(encoder: Encoder, value: TestStartEvent): Unit =
    encoder.writeLong(value.getStartTime)
    nullableIdSerializer.write(encoder, value.getParentId)

  override def read(decoder: Decoder): TestStartEvent =
    val time: Long = decoder.readLong
    val parentId: Object = nullableIdSerializer.read(decoder)

    TestStartEvent(
      time,
      parentId
    )
