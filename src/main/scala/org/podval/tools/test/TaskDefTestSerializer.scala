package org.podval.tools.test

import org.gradle.internal.serialize.{Decoder, Encoder, Serializer}
import org.podval.tools.test.serializer.{FrameworkSerializer, IdSerializer, NullableSerializer, TaskDefSerializer}

final class TaskDefTestSerializer(
  idSerializer: IdSerializer,
  nullableIdSerializer: NullableSerializer[Object],
  frameworkSerializer: FrameworkSerializer,
  taskDefSerializer: TaskDefSerializer
) extends Serializer[TaskDefTest]:

  override def write(encoder: Encoder, value: TaskDefTest): Unit =
    nullableIdSerializer.write(encoder, value.getParentId)
    idSerializer.write(encoder, value.getId)
    frameworkSerializer.write(encoder, value.framework)
    taskDefSerializer.write(encoder, value.taskDef)

  override def read(decoder: Decoder): TaskDefTest = TaskDefTest(
    getParentId = nullableIdSerializer.read(decoder),
    getId = idSerializer.read(decoder),
    framework = frameworkSerializer.read(decoder),
    taskDef = taskDefSerializer.read(decoder)
  )
