package org.podval.tools.test

import org.gradle.api.internal.tasks.testing.{DefaultNestedTestSuiteDescriptor, DefaultTestClassDescriptor,
  DefaultTestClassRunInfo, DefaultTestDescriptor, DefaultTestFailure, DefaultTestMethodDescriptor, DefaultTestOutputEvent,
  DefaultTestSuiteDescriptor, TestCompleteEvent, TestStartEvent}
import org.gradle.api.internal.tasks.testing.worker.{TestEventSerializer, WorkerTestClassProcessor}
import org.gradle.internal.id.CompositeIdGenerator
import org.gradle.internal.serialize.{BaseSerializerFactory, Serializer, SerializerRegistry}
import org.podval.tools.test.serializer.*

object TestSerializerRegistry:

  def create: SerializerRegistry =
    val result: SerializerRegistry = TestEventSerializer.create()

    // TODO I use composite ids of unlimited length; not sure how Gradle does with fixed length of 2 - what about nested tests?
    // See how they use DefaultNestedTestSuiteDescriptor
    // I'd love to just add TaskDefTestSerializer, but since serializers from TestEventSerializer hard-code
    // the CompositeIdSerializer they, I have to replicate those that do use CompositeIdSerializer;
    // for now, I just do them all...
    result.register(classOf[CompositeIdGenerator.CompositeId], new CompositeIdSerializer)
    result.register(classOf[TaskDefTest], new TaskDefTestSerializer)
    result.register(classOf[DefaultTestClassRunInfo], new DefaultTestClassRunInfoSerializer)
    result.register(classOf[DefaultNestedTestSuiteDescriptor], new DefaultNestedTestSuiteDescriptorSerializer)
    result.register(classOf[DefaultTestSuiteDescriptor], new DefaultTestSuiteDescriptorSerializer)
    result.register(classOf[WorkerTestClassProcessor.WorkerTestSuiteDescriptor], new WorkerTestSuiteDescriptorSerializer)
    result.register(classOf[DefaultTestClassDescriptor], new DefaultTestClassDescriptorSerializer)
    result.register(classOf[DefaultTestMethodDescriptor], new DefaultTestMethodDescriptorSerializer)
    result.register(classOf[DefaultTestDescriptor], new DefaultTestDescriptorSerializer)
    result.register(classOf[TestStartEvent], new TestStartEventSerializer)
    result.register(classOf[TestCompleteEvent], new TestCompleteEventSerializer)
    result.register(classOf[DefaultTestOutputEvent], new DefaultTestOutputEventSerializer)
    result.register(classOf[Throwable], new BaseSerializerFactory().getSerializerFor(classOf[Throwable]))
    result.register(classOf[DefaultTestFailure], new DefaultTestFailureSerializer)

    result

    // Note: org.gradle.internal.remote.internal.hub.DefaultMethodArgsSerializer
    // seems to make a decision based on the type of the first argument of a method
    // (and somehow I didn't even see calls to canSerialize() for those...);
    // in TestResultProcessor, first argument is often an id - so, when I was using not CompositeIds
    // but strings etc., I had to attract attention by registering a serializer for AnyRef -
    // and then I had to derive my registry from the default and override build() to force the use of IdSerializer
    // when baseType.getName is "java.lang.Object".
    // None of this is needed now :)
