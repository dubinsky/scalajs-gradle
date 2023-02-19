package org.podval.tools.test.serializer

import org.gradle.api.internal.tasks.testing.DefaultTestOutputEvent
import org.gradle.api.tasks.testing.TestOutputEvent
import org.gradle.internal.serialize.{BaseSerializerFactory, Decoder, Encoder, Serializer}

final class DefaultTestOutputEventSerializer extends Serializer[DefaultTestOutputEvent]:
  private val destinationSerializer: Serializer[TestOutputEvent.Destination] =
    new BaseSerializerFactory().getSerializerFor(classOf[TestOutputEvent.Destination])

  override def read(decoder: Decoder): DefaultTestOutputEvent =
    val destination: TestOutputEvent.Destination = destinationSerializer.read(decoder)
    val message: String = decoder.readString

    DefaultTestOutputEvent(
      destination,
      message
    )

  override def write(encoder: Encoder, value: DefaultTestOutputEvent): Unit =
    destinationSerializer.write(encoder, value.getDestination)
    encoder.writeString(value.getMessage)
