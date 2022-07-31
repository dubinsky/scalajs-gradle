package org.podval.tools.test

import org.gradle.internal.service.ServiceRegistry
import org.gradle.internal.time.Clock
import java.io.File

// Note: this one is serialized, so I am using serializable types for parameters
final class WorkerTestClassProcessorFactory(
  groupByFramework: Boolean,
  testClassPath: Array[File],
  runningInIntelliJIdea: Boolean,
  testTagging: TestTagging
) extends org.gradle.api.internal.tasks.testing.WorkerTestClassProcessorFactory
  with Serializable:

  override def create(serviceRegistry: ServiceRegistry): TestClassProcessor = TestClassProcessor(
    groupByFramework = groupByFramework,
    isForked = true,
    testClassPath = testClassPath,
    runningInIntelliJIdea = runningInIntelliJIdea,
    testTagging = testTagging,
    clock = serviceRegistry.get(classOf[Clock])
  )
