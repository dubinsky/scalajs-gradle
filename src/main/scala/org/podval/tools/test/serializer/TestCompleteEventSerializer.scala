package org.podval.tools.test.serializer

import org.gradle.api.internal.tasks.testing.TestCompleteEvent
import org.gradle.api.tasks.testing.TestResult.ResultType
import org.gradle.internal.serialize.{Decoder, Encoder, Serializer}

final class TestCompleteEventSerializer(
  nullableResultTypeSerializer: Serializer[ResultType]
) extends Serializer[TestCompleteEvent]:

  override def write(encoder: Encoder, value: TestCompleteEvent): Unit =
    encoder.writeLong(value.getEndTime)
    nullableResultTypeSerializer.write(encoder, value.getResultType)

  override def read(decoder: Decoder): TestCompleteEvent =
    val endTime: Long = decoder.readLong
    val result: ResultType = nullableResultTypeSerializer.read(decoder)

    TestCompleteEvent(
      endTime,
      result
    )
