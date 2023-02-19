package org.podval.tools.test.serializer

import org.gradle.api.internal.tasks.testing.{DefaultTestFailure, DefaultTestFailureDetails}
import org.gradle.api.tasks.testing.TestFailure
import org.gradle.internal.serialize.{BaseSerializerFactory, Decoder, Encoder, Serializer}
import scala.jdk.CollectionConverters.*

final class DefaultTestFailureSerializer extends Serializer[DefaultTestFailure]:
  private val throwableSerializer: Serializer[Throwable] = new BaseSerializerFactory().getSerializerFor(classOf[Throwable])

  override def write(encoder: Encoder, value: DefaultTestFailure): Unit =
    throwableSerializer.write(encoder, value.getRawFailure)
    encoder.writeNullableString(value.getDetails.getMessage)
    encoder.writeString(value.getDetails.getClassName)
    encoder.writeString(value.getDetails.getStacktrace)
    encoder.writeBoolean(value.getDetails.isAssertionFailure)
    encoder.writeNullableString(value.getDetails.getExpected)
    encoder.writeNullableString(value.getDetails.getActual)
    encoder.writeSmallInt(value.getCauses.size)
    for cause <- value.getCauses.asScala do write(encoder, cause.asInstanceOf[DefaultTestFailure])

  override def read(decoder: Decoder): DefaultTestFailure =
    val rawFailure: Throwable = throwableSerializer.read(decoder)
    val message: String = decoder.readNullableString()
    val className: String = decoder.readString()
    val stacktrace: String = decoder.readString()
    val isAssertionFailure: Boolean = decoder.readBoolean()
    val expected: String = decoder.readNullableString()
    val actual: String = decoder.readNullableString()
    val numOfCauses: Int = decoder.readSmallInt()
    val causes: Seq[TestFailure] = 0.until(numOfCauses).map(_ => read(decoder))

    new DefaultTestFailure(
      rawFailure,
      new DefaultTestFailureDetails(
        message,
        className,
        stacktrace,
        isAssertionFailure,
        expected,
        actual
      ),
      causes.asJava
    )
