package org.podval.tools.test.run

import org.gradle.api.internal.tasks.testing.{TestDefinition, TestDefinitionProcessor, TestResultProcessor}
import org.podval.tools.test.taskdef.TestClassRun

class ReadTestDefinitionProcessor[D <: TestDefinition](delegate: RunTestDefinitionProcessor[D]) extends TestDefinitionProcessor[D]:
  override def startProcessing(testResultProcessor: TestResultProcessor): Unit = delegate.startProcessing(testResultProcessor)
  override def stopNow(): Unit = delegate.stopNow()
  override def stop(): Unit = delegate.stop()

  override def processTestDefinition(testDefinition: D): Unit = delegate.processTestDefinition(
    TestClassRun.read(testDefinition.getId).asInstanceOf[D]
  )
