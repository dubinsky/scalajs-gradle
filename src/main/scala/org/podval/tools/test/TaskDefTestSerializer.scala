package org.podval.tools.test

import org.gradle.internal.id.CompositeIdGenerator.CompositeId
import org.gradle.internal.serialize.{Decoder, Encoder, Serializer}
import org.podval.tools.test.serializer.{CompositeIdSerializer, FrameworkSerializer, TaskDefSerializer}

final class TaskDefTestSerializer extends Serializer[TaskDefTest]:
  private val idSerializer: CompositeIdSerializer = new CompositeIdSerializer
  private val frameworkSerializer: FrameworkSerializer = new FrameworkSerializer
  private val taskDefSerializer: TaskDefSerializer = new TaskDefSerializer

  override def write(encoder: Encoder, value: TaskDefTest): Unit =
    idSerializer.write(encoder, value.id.asInstanceOf[CompositeId])
    frameworkSerializer.write(encoder, value.framework)
    taskDefSerializer.write(encoder, value.taskDef)

  override def read(decoder: Decoder): TaskDefTest = TaskDefTest(
    id = idSerializer.read(decoder),
    framework = frameworkSerializer.read(decoder),
    taskDef = taskDefSerializer.read(decoder)
  )
