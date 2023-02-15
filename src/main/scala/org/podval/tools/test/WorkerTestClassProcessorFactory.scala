package org.podval.tools.test

import org.gradle.api.logging.LogLevel
import org.gradle.internal.service.ServiceRegistry
import org.gradle.internal.time.Clock
import java.io.File

// Note: this one is serialized, so I am using serializable types for parameters
final class WorkerTestClassProcessorFactory(
  groupByFramework: Boolean,
  testClassPath: Array[File],
  runningInIntelliJIdea: Boolean,
  testTagsFilter: TestTagsFilter,
  logLevelEnabled: LogLevel
) extends org.gradle.api.internal.tasks.testing.WorkerTestClassProcessorFactory
  with Serializable:

  override def create(serviceRegistry: ServiceRegistry): TestClassProcessor = create(
    isForked = true,
    clock = serviceRegistry.get(classOf[Clock])
  )

  def create(clock: Clock): TestClassProcessor = create(
    isForked = false,
    clock = clock
  )

  private def create(
    isForked: Boolean,
    clock: Clock
  ): TestClassProcessor = TestClassProcessor(
    frameworkRuns = FrameworkRuns(
      isForked,
      testClassPath,
      testTagsFilter
    ),
    clock = clock,
    groupByFramework = groupByFramework,
    runningInIntelliJIdea = runningInIntelliJIdea,
    testTagsFilter = testTagsFilter,
    logLevelEnabled = logLevelEnabled
  )
