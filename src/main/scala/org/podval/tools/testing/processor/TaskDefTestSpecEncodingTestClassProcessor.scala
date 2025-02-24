package org.podval.tools.testing.processor

import org.gradle.api.internal.tasks.testing.{DefaultTestClassRunInfo, TestClassProcessor, TestClassRunInfo, TestResultProcessor}
import org.podval.tools.testing.TaskDefTestSpec

class TaskDefTestSpecEncodingTestClassProcessor(delegate: TestClassProcessor) extends TestClassProcessor:
  override def startProcessing(testResultProcessor: TestResultProcessor): Unit = delegate.startProcessing(testResultProcessor)
  override def stopNow(): Unit = delegate.stopNow()
  override def stop(): Unit = delegate.stop()

  override def processTestClass(testClassRunInfo: TestClassRunInfo): Unit =
    delegate.processTestClass(DefaultTestClassRunInfo(testClassRunInfo.asInstanceOf[TaskDefTestSpec].write))
