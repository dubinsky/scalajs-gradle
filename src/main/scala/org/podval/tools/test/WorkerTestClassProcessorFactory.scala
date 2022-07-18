package org.podval.tools.test

import org.gradle.internal.service.ServiceRegistry
import org.gradle.internal.time.Clock
import java.io.File

final class WorkerTestClassProcessorFactory(
  testClassPath: Array[File],
  runningInIntelliJIdea: Boolean,
  includeTags: Array[String],
  excludeTags: Array[String]
) extends org.gradle.api.internal.tasks.testing.WorkerTestClassProcessorFactory with Serializable:
  override def create(serviceRegistry: ServiceRegistry): TestClassProcessor = TestClassProcessor(
    testClassPath = testClassPath,
    runningInIntelliJIdea = runningInIntelliJIdea,
    includeTags = includeTags,
    excludeTags = includeTags,
    clock = serviceRegistry.get(classOf[Clock])
  )
