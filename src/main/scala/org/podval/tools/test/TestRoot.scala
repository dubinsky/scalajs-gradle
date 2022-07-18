package org.podval.tools.test

import org.gradle.api.internal.tasks.testing.{TestClassProcessor, TestResultProcessor}
import org.gradle.internal.time.Clock
import org.gradle.internal.work.WorkerLeaseService
import org.opentorah.build.Gradle
import sbt.testing.Framework
import java.io.File
import TestResultProcessorEx.*

// Note: I am following the split between
// org.gradle.api.internal.tasks.testing.detection.DefaultTestExecuter and
// org.gradle.api.internal.tasks.testing.processors.TestMainAction.
class TestRoot(
  testEnvironment: TestEnvironment,
  loadedFrameworks: List[Framework],
  testClassScanner: Runnable,
  testClassProcessor: TestClassProcessor,
  testResultProcessor: TestResultProcessor,
  clock: Clock,
  sbtClassPath: Iterable[File],
  workerLeaseService: WorkerLeaseService
) extends Runnable:

  override def run(): Unit =
    val startTime: Long = clock.getCurrentTime
    val rootTest: RootTest = new RootTest

    testResultProcessor.started(
      test = rootTest,
      startTime = startTime
    )

    val frameworkTests: List[FrameworkTest] = for framework <- loadedFrameworks yield FrameworkTest(
      parentId = rootTest.getId,
      framework = framework
    )

    for frameworkTest: FrameworkTest <- frameworkTests do testResultProcessor.started(
      test = frameworkTest,
      startTime = startTime
    )

    try
      testClassProcessor.startProcessing(testResultProcessor)
      try
        Gradle.addToClassPath(this, sbtClassPath)
        testClassScanner.run()
      finally
        // Release worker lease while waiting for tests to complete
        workerLeaseService.blocking(() => testClassProcessor.stop())
    finally
        val endTime: Long = clock.getCurrentTime
        for frameworkTest: FrameworkTest <- frameworkTests do testResultProcessor.completed(
          test = frameworkTest,
          endTime = endTime
        )

        testResultProcessor.completed(
          test = rootTest,
          endTime = endTime
        )

        testEnvironment.close()
