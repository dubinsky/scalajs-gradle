package org.podval.tools.testing.processors

import org.gradle.api.internal.tasks.testing.{TestClassRunInfo, TestResultProcessor}

// Note: The only place I can wrap this around the delegate
// in a way that does not interfere with the RunPreviousFailedFirstTestClassProcessor
// is in the TestFrameworkDetector.startDetection()
class SortingTestClassProcessor(
  delegate: org.gradle.api.internal.tasks.testing.TestClassProcessor,
  isEnabled: Boolean
) extends org.gradle.api.internal.tasks.testing.TestClassProcessor:
  override def startProcessing(testResultProcessor: TestResultProcessor): Unit = delegate.startProcessing(testResultProcessor)
  override def stopNow(): Unit = delegate.stopNow()
  private var pending: List[TestClassRunInfo] = List.empty
  override def processTestClass(testClassRunInfo: TestClassRunInfo): Unit =
    if !isEnabled
    then delegate.processTestClass(testClassRunInfo)
    else pending = testClassRunInfo +: pending
  override def stop(): Unit =
    pending.sortBy(_.getTestClassName).foreach(delegate.processTestClass)
    delegate.stop()
