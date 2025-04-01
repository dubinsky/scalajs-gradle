package org.podval.tools.test.run

import org.gradle.api.internal.tasks.testing.{DefaultTestClassRunInfo, TestClassProcessor, TestClassRunInfo, TestResultProcessor}
import org.podval.tools.test.taskdef.TestClassRun

class WriteTestClassProcessor(delegate: TestClassProcessor) extends TestClassProcessor:
  override def startProcessing(testResultProcessor: TestResultProcessor): Unit = delegate.startProcessing(testResultProcessor)
  override def stopNow(): Unit = delegate.stopNow()
  override def stop(): Unit = delegate.stop()

  override def processTestClass(testClassRunInfo: TestClassRunInfo): Unit =
    val testClassRunNonForking: TestClassRun = testClassRunInfo.asInstanceOf[TestClassRun]
    delegate.processTestClass(
      DefaultTestClassRunInfo(
        TestClassRun.write(
          testClassRunNonForking
        )
      )
    )
