package org.podval.tools.test.serializer

import org.gradle.api.internal.tasks.testing.DefaultTestOutputEvent
import org.gradle.api.tasks.testing.TestOutputEvent
import org.gradle.internal.serialize.{Decoder, Encoder, Serializer}

final class TestOutputEventSerializer(
  destinationSerializer: Serializer[TestOutputEvent.Destination]
) extends Serializer[TestOutputEvent]:

  override def read(decoder: Decoder): TestOutputEvent =
    val destination: TestOutputEvent.Destination = destinationSerializer.read(decoder)
    val message: String = decoder.readString

    DefaultTestOutputEvent(
      destination,
      message
    )

  override def write(encoder: Encoder, value: TestOutputEvent): Unit =
    destinationSerializer.write(encoder, value.getDestination)
    encoder.writeString(value.getMessage)
