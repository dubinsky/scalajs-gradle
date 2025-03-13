package org.podval.tools.test.run

import org.gradle.api.internal.tasks.testing.{TestClassProcessor, TestClassRunInfo, TestResultProcessor}
import org.podval.tools.test.taskdef.TestClassRunForking

class ReadTestClassProcessor(delegate: RunTestClassProcessor) extends TestClassProcessor:
  override def startProcessing(testResultProcessor: TestResultProcessor): Unit = delegate.startProcessing(testResultProcessor)
  override def stopNow(): Unit = delegate.stopNow()
  override def stop(): Unit = delegate.stop()

  override def processTestClass(testClassRunInfo: TestClassRunInfo): Unit =
    delegate.processTestClass(
      TestClassRunForking.read(testClassRunInfo.getTestClassName)
    )
