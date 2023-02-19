package org.podval.tools.test.serializer

import org.gradle.api.internal.tasks.testing.worker.WorkerTestClassProcessor
import org.gradle.internal.id.CompositeIdGenerator.CompositeId
import org.gradle.internal.serialize.{Decoder, Encoder, Serializer}

final class WorkerTestSuiteDescriptorSerializer extends Serializer[WorkerTestClassProcessor.WorkerTestSuiteDescriptor]:
  private val idSerializer: CompositeIdSerializer = new CompositeIdSerializer
  
  override def read(decoder: Decoder): WorkerTestClassProcessor.WorkerTestSuiteDescriptor =
    val id: CompositeId = idSerializer.read(decoder)
    val name: String = decoder.readString

    WorkerTestClassProcessor.WorkerTestSuiteDescriptor(
      id,
      name
    )

  override def write(encoder: Encoder, value: WorkerTestClassProcessor.WorkerTestSuiteDescriptor): Unit =
    idSerializer.write(encoder, value.getId.asInstanceOf[CompositeId])
    encoder.writeString(value.getName)
