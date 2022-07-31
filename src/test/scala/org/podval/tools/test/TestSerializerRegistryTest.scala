package org.podval.tools.test

import org.gradle.internal.id.CompositeIdGenerator.CompositeId
import org.gradle.internal.serialize.{InputStreamBackedDecoder, OutputStreamBackedEncoder, Serializer}
import org.podval.tools.test.framework.ScalaTest
import org.podval.tools.test.serializer.{AnnotatedFingerprintImpl, FrameworkSerializer, IdSerializer, NullableSerializer}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sbt.testing.{SuiteSelector, TaskDef, TestSelector}
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

class TestSerializerRegistryTest extends AnyFlatSpec, Matchers:
  private val serializerRegistry: TestSerializerRegistry = new TestSerializerRegistry

  def roundTrip[T](serializer: Serializer[T], value: T): T =
    val os: ByteArrayOutputStream = new ByteArrayOutputStream
    serializer.write(OutputStreamBackedEncoder(os), value)
    serializer.read(InputStreamBackedDecoder(ByteArrayInputStream(os.toByteArray)))

  def check[T](serializer: Serializer[T], value: T): Unit =
    given CanEqual[T, T] = CanEqual.derived
    roundTrip(serializer, value) shouldBe value

  "IdSerializer.nullable" should "work" in {
    val nullableIdSerializer: NullableSerializer[Object] = serializerRegistry.nullableIdSerializer

    check(nullableIdSerializer, null)
    check(nullableIdSerializer, "ab")
    check(nullableIdSerializer, Long.box(3))
    check(nullableIdSerializer, CompositeId("x", 1l))
  }

  "TaskDefTestSerializer" should "work" in {
    val taskDefTestSerializer: Serializer[TaskDefTest] = serializerRegistry.taskDefTestSerializer
    val value: TaskDefTest = TaskDefTest(
      getParentId = "ScalaTest",
      getId = java.lang.Long.valueOf(1),
      framework = FrameworkSerializer.instantiate(ScalaTest.implementationClassName),
      taskDef = TaskDef(
        "X",
        AnnotatedFingerprintImpl(
          annotationName = "myann",
          isModule = false
        ),
        false,
        Array(new SuiteSelector, TestSelector("sdf"))
      )
    )

    check(taskDefTestSerializer, value)
  }
