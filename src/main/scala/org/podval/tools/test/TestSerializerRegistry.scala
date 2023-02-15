package org.podval.tools.test

import org.gradle.api.internal.tasks.testing.{TestCompleteEvent, TestDescriptorInternal, TestStartEvent}
import org.gradle.api.tasks.testing.{TestOutputEvent, TestFailure}
import org.gradle.api.tasks.testing.TestResult.ResultType
import org.gradle.internal.serialize.{BaseSerializerFactory, DefaultSerializerRegistry, Serializer, SerializerRegistry}
import org.podval.tools.test.serializer.*
import sbt.testing.Selector

// Note: based on org.gradle.api.internal.tasks.testing.worker.TestEventSerializer
// TODO maybe I can *add* my classes to the registries instead of copying and modifying Gradle classes?
// And if not, I should file a pull request against Gradle to make registries overridable...
// TODO can I throw an exception when the class is not serializable?
final class TestSerializerRegistry extends DefaultSerializerRegistry:
  private val baseSerializerFactory: BaseSerializerFactory = new BaseSerializerFactory

  val idSerializer: IdSerializer =
    new IdSerializer

  val nullableIdSerializer: NullableSerializer[Object] =
    NullableSerializer(idSerializer)

  val testDescriptorInternalSerializer: TestDescriptorInternalSerializer =
    new TestDescriptorInternalSerializer(idSerializer)

  val outputDestinationSerializer: Serializer[TestOutputEvent.Destination] =
    baseSerializerFactory.getSerializerFor(classOf[TestOutputEvent.Destination])

  val nullableResultTypeSerializer: Serializer[ResultType] =
    NullableSerializer(baseSerializerFactory.getSerializerFor(classOf[ResultType]))

  val frameworkSerializer: FrameworkSerializer = new FrameworkSerializer

  val selectorsSerializer: ArraySerializer[Selector] =
    ArraySerializer(new SelectorSerializer)

  val taskDefSerializer: TaskDefSerializer = TaskDefSerializer(
    fingerprintSerializer = new FingerprintSerializer,
    selectorsSerializer = selectorsSerializer
  )

  val taskDefTestSerializer: TaskDefTestSerializer = TaskDefTestSerializer(
    idSerializer,
    nullableIdSerializer,
    frameworkSerializer,
    taskDefSerializer
  )

  val throwableSerializer: Serializer[Throwable] = baseSerializerFactory.getSerializerFor(classOf[Throwable])

  register(classOf[TaskDefTest], taskDefTestSerializer)

  register(classOf[TestStartEvent], TestStartEventSerializer(nullableIdSerializer))
  register(classOf[TestCompleteEvent], TestCompleteEventSerializer(nullableResultTypeSerializer))
  register(classOf[TestOutputEvent], TestOutputEventSerializer(outputDestinationSerializer))
  register(classOf[TestFailure], TestFailureSerializer(throwableSerializer))

  register(classOf[Throwable], throwableSerializer)

  // Note: org.gradle.internal.remote.internal.hub.DefaultMethodArgsSerializer
  // seems to make a decision based on the type of the first argument of a method
  // (and somehow I didn't even see calls to canSerialize() for those...);
  // in TestResultProcessor, first argument is often an id - so, I am attracting attention:
  register(classOf[Object], idSerializer)

  // Note: some TestResultProcessor subclasses wrap things in TestDescriptorInternal,
  // and I need to be able to serialize it:
  register(classOf[TestDescriptorInternal], testDescriptorInternalSerializer)

  // TestClassSerializer is also a TestDescriptorInternal,
  // and everything is an Object, so I need to manually disambiguate:
  override def build[T](baseType: Class[T]): Serializer[T] = baseType.getName match
    case "org.gradle.api.internal.tasks.testing.TestDescriptorInternal" =>
      testDescriptorInternalSerializer.asInstanceOf[Serializer[T]]
    case "java.lang.Object" =>
      idSerializer                    .asInstanceOf[Serializer[T]]
    case _ =>
      super.build(baseType)

object TestSerializerRegistry:
  def create: SerializerRegistry = new TestSerializerRegistry
