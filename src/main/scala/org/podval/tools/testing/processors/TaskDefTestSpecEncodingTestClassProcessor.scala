package org.podval.tools.testing.processors

import org.gradle.api.internal.tasks.testing.{DefaultTestClassRunInfo, TestClassProcessor, TestClassRunInfo, TestResultProcessor}
import org.podval.tools.testing.serializer.{TaskDefTestSpec, TaskDefTestSpecWriter}

class TaskDefTestSpecEncodingTestClassProcessor(delegate: TestClassProcessor) extends TestClassProcessor:
  override def startProcessing(testResultProcessor: TestResultProcessor): Unit = delegate.startProcessing(testResultProcessor)
  override def stopNow(): Unit = delegate.stopNow()
  override def stop(): Unit = delegate.stop()

  override def processTestClass(testClassRunInfo: TestClassRunInfo): Unit =
    delegate.processTestClass(DefaultTestClassRunInfo(TaskDefTestSpecWriter.write(testClassRunInfo.asInstanceOf[TaskDefTestSpec])))
