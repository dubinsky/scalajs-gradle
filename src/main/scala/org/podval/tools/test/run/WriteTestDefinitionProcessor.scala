package org.podval.tools.test.run

import org.gradle.api.internal.tasks.testing.{ClassTestDefinition, TestDefinition, TestDefinitionProcessor,
  TestResultProcessor}

class WriteTestDefinitionProcessor[D <: TestDefinition](delegate: TestDefinitionProcessor[D]) extends TestDefinitionProcessor[D]:
  override def startProcessing(testResultProcessor: TestResultProcessor): Unit = delegate.startProcessing(testResultProcessor)
  override def stopNow(): Unit = delegate.stopNow()
  override def stop(): Unit = delegate.stop()

  override def processTestDefinition(testDefinition: D): Unit =
    val testClassRun: TestClassRun = testDefinition.asInstanceOf[TestClassRun]
    delegate.processTestDefinition(
      ClassTestDefinition(
        TestClassRun.write(
          testClassRun
        )
      ).asInstanceOf[D]
    )
