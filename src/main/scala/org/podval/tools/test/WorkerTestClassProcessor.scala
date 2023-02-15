package org.podval.tools.test

import org.gradle.api.internal.tasks.testing.{TestClassProcessor, TestClassRunInfo, TestResultProcessor}
import org.gradle.api.internal.tasks.testing.processors.{CaptureTestOutputTestResultProcessor,
  DefaultStandardOutputRedirector, StandardOutputRedirector}

// Note: translated from org.gradle.api.internal.tasks.testing.worker.WorkerTestClassProcessor and modified:
// - not to wrap everything in WorkerTestSuiteDescriptor;
// - not to create test ids;
// - not to inherit from SuiteTestClassProcessor to:
//   - not to emit suite events;
//   - not to use AttachParentTestResultProcessor (I have my parentIds set);
// - copy some stuff from SuiteTestClassProcessor to:
//   - use CaptureTestOutputTestResultProcessor to capture output.
//
// Now that forking of the tests works, this can be rolled back to the original,
// but it does not seem to be worth it:
// - I'll need serializers for WorkerTestSuiteDescriptor and CompositeId (where both scope and id are Longs);
// - I do not think I want the per-worker synthetic suites;
// - I already have parentId for my suites, which I'll need to null out to conform to the expectations of the Gradle code.
final class WorkerTestClassProcessor(processor: TestClassProcessor) extends TestClassProcessor:

  override def startProcessing(testResultProcessor: TestResultProcessor): Unit =
    val resultProcessor: TestResultProcessor =
//      CaptureTestOutputTestResultProcessor(
        testResultProcessor
//        ,
//        new DefaultStandardOutputRedirector // TODO JUL?
//      )
    processor.startProcessing(resultProcessor)

  override def processTestClass(testClass: TestClassRunInfo): Unit =
    processor.processTestClass(testClass)

  override def stop(): Unit =
    processor.stop()

  override def stopNow(): Unit =
    throw UnsupportedOperationException("stopNow() should not be invoked on remote worker TestClassProcessor")
