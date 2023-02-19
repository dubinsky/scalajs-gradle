package org.podval.tools.test.serializer

import org.gradle.api.internal.tasks.testing.TestStartEvent
import org.gradle.internal.id.CompositeIdGenerator.CompositeId
import org.gradle.internal.serialize.{Decoder, Encoder, Serializer}

final class TestStartEventSerializer extends Serializer[TestStartEvent]:
  val nullableIdSerializer: NullableSerializer[CompositeId] = NullableSerializer(new CompositeIdSerializer)

  override def write(encoder: Encoder, value: TestStartEvent): Unit =
    encoder.writeLong(value.getStartTime)
    nullableIdSerializer.write(encoder, value.getParentId.asInstanceOf[CompositeId])

  override def read(decoder: Decoder): TestStartEvent =
    val time: Long = decoder.readLong
    val parentId: CompositeId = nullableIdSerializer.read(decoder)

    TestStartEvent(
      time,
      parentId
    )
