package org.podval.tools.test.run

import org.gradle.api.internal.tasks.testing.{TestDefinition, TestDefinitionProcessor, TestResultProcessor}

// The only place I can wrap this around the delegate
// in a way that does not interfere with the RunPreviousFailedFirstTestDefinitionProcessor
// is in the TestFrameworkDetector.startDetection()
class SortingTestDefinitionProcessor[D <: TestDefinition](
  delegate: TestDefinitionProcessor[D],
  isEnabled: Boolean
) extends TestDefinitionProcessor[D]:
  override def startProcessing(testResultProcessor: TestResultProcessor): Unit = delegate.startProcessing(testResultProcessor)
  override def stopNow(): Unit = delegate.stopNow()
  private var pending: List[D] = List.empty
  override def processTestDefinition(testDefinition: D): Unit =
    if !isEnabled
    then delegate.processTestDefinition(testDefinition)
    else pending = testDefinition +: pending
  override def stop(): Unit =
    pending.sortBy(_.getId).foreach(delegate.processTestDefinition)
    delegate.stop()
