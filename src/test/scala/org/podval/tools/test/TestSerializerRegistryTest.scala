package org.podval.tools.test

import org.gradle.api.internal.tasks.testing.DefaultTestMethodDescriptor
import org.gradle.internal.id.CompositeIdGenerator.CompositeId
import org.gradle.internal.serialize.{InputStreamBackedDecoder, OutputStreamBackedEncoder, Serializer, SerializerRegistry}
import org.podval.tools.test.framework.ScalaTest
import org.podval.tools.test.serializer.{AnnotatedFingerprintImpl, CompositeIdSerializer, FrameworkSerializer, NullableSerializer}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sbt.testing.{SuiteSelector, TaskDef, TestSelector}
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

class TestSerializerRegistryTest extends AnyFlatSpec, Matchers:
  private val serializerRegistry: SerializerRegistry = TestSerializerRegistry.create

  def roundTrip[T](serializer: Serializer[T], value: T): T =
    val os: ByteArrayOutputStream = new ByteArrayOutputStream
    serializer.write(OutputStreamBackedEncoder(os), value)
    serializer.read(InputStreamBackedDecoder(ByteArrayInputStream(os.toByteArray)))

  def check[T](serializer: Serializer[T], value: T): Unit =
    given CanEqual[T, T] = CanEqual.derived
    val result: T = roundTrip(serializer, value)
//    println(s"value=$value; result=$result")
    result shouldBe value

  // Note: Gradle test descriptor classes do not define equals()...
  def checkToString[T](serializer: Serializer[T], value: T): Unit =
    given CanEqual[T, T] = CanEqual.derived

    val result: T = roundTrip(serializer, value)
    //    println(s"value=$value; result=$result")
    result.toString shouldBe value.toString

  "CompositeIdSerializer" should "work" in {
    val serializer = new CompositeIdSerializer
    check(serializer, CompositeId(1L, 2L))
    check(serializer, CompositeId(CompositeId(1L, 2L), 3L))
    check(serializer, CompositeId(CompositeId(CompositeId(1L, 2L), 3L), 4L))

    val nullableSerializer = NullableSerializer(serializer)
    check(nullableSerializer, null)
    check(nullableSerializer, CompositeId(1L, 2L))
    check(nullableSerializer, CompositeId(CompositeId(1L, 2L), 3L))
  }

  "DefaultTestMethodDescriptorSerializer" should "work" in {
    val i = DefaultTestMethodDescriptor(
      CompositeId(CompositeId(0L, 1L), 1L),
      "org.podval.tools.test.ScalaTestTest",
      "2*2 success should pass"
    )

    serializerRegistry.canSerialize(classOf[DefaultTestMethodDescriptor]) shouldBe true
    val serializer: Serializer[DefaultTestMethodDescriptor] = serializerRegistry.build(classOf[DefaultTestMethodDescriptor])
    checkToString(serializer, i)
  }

  "TaskDefTestSerializer" should "work" in {
    val taskDefTestSerializer: Serializer[TaskDefTest] = new TaskDefTestSerializer
    val value: TaskDefTest = TaskDefTest(
      id = CompositeId(1L, 2L),
      framework = ScalaTest.instantiate,
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
