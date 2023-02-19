package org.podval.tools.test

import org.gradle.api.logging.LogLevel
import org.gradle.internal.service.ServiceRegistry
import org.gradle.internal.time.Clock
import java.io.File

// Note: this one is serialized, so I am using serializable types for parameters
final class WorkerTestClassProcessorFactory(
  isForked: Boolean,
  testClassPath: Array[File],
  runningInIntelliJIdea: Boolean,
  testTagsFilter: TestTagsFilter,
  logLevelEnabled: LogLevel,
  rootTestSuiteId: AnyRef
) extends org.gradle.api.internal.tasks.testing.WorkerTestClassProcessorFactory with Serializable:

  override def create(serviceRegistry: ServiceRegistry): TestClassProcessor =
    create(serviceRegistry.get(classOf[Clock]))

  def create(clock: Clock): TestClassProcessor =
    TestClassProcessor(
      frameworkRuns = FrameworkRuns(
        isForked,
        testClassPath,
        testTagsFilter
      ),
      clock = clock,
      runningInIntelliJIdea = runningInIntelliJIdea,
      testTagsFilter = testTagsFilter,
      logLevelEnabled = logLevelEnabled,
      rootTestSuiteId = rootTestSuiteId
    )
