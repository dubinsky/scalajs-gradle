package org.podval.tools.test.run

import org.gradle.api.internal.tasks.testing.{DefaultTestClassRunInfo, TestClassProcessor, TestClassRunInfo, TestResultProcessor}
import org.podval.tools.test.taskdef.{TestClassRunForking, TestClassRunNonForking}

class WriteTestClassProcessor(delegate: TestClassProcessor) extends TestClassProcessor:
  override def startProcessing(testResultProcessor: TestResultProcessor): Unit = delegate.startProcessing(testResultProcessor)
  override def stopNow(): Unit = delegate.stopNow()
  override def stop(): Unit = delegate.stop()

  override def processTestClass(testClassRunInfo: TestClassRunInfo): Unit =
    val testClassRunNonForking: TestClassRunNonForking = testClassRunInfo.asInstanceOf[TestClassRunNonForking]
    delegate.processTestClass(
      DefaultTestClassRunInfo(
        TestClassRunForking.write(
          TestClassRunForking(
            frameworkName = testClassRunNonForking.frameworkName,
            taskDef = testClassRunNonForking.taskDef
          )
        )
      )
    )

