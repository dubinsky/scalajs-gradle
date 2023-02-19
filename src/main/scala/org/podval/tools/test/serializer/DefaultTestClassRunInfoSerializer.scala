package org.podval.tools.test.serializer

import org.gradle.api.internal.tasks.testing.DefaultTestClassRunInfo
import org.gradle.internal.serialize.{Decoder, Encoder, Serializer}

final class DefaultTestClassRunInfoSerializer extends Serializer[DefaultTestClassRunInfo]:
  override def read(decoder: Decoder): DefaultTestClassRunInfo =
    DefaultTestClassRunInfo(decoder.readString)

  override def write(encoder: Encoder, value: DefaultTestClassRunInfo): Unit =
    encoder.writeString(value.getTestClassName)
