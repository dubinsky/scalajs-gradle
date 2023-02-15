package org.podval.tools.test

import org.gradle.api.internal.tasks.testing.{TestCompleteEvent, TestDescriptorInternal, TestResultProcessor, TestStartEvent}
import org.gradle.api.tasks.testing.TestResult.ResultType

abstract class Test extends TestDescriptorInternal:
  final override def getParent: Test = null
  final override def getDisplayName: String = getName
  final override def getClassDisplayName: String = getClassName

  def getParentId: Object

  def started(
    time: Long,
    testResultProcessor: TestResultProcessor
  ): Unit = testResultProcessor.started(
    this,
    TestStartEvent(
      time,
      getParentId
    )
  )
  
  def completed(
    time: Long,
    resultType: ResultType,
    testResultProcessor: TestResultProcessor
  ): Unit = testResultProcessor.completed(
    getId,
    TestCompleteEvent(
      time,
      resultType
    )
  )

