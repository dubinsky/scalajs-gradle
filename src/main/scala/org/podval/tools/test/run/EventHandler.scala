package org.podval.tools.test.run

import org.gradle.api.tasks.testing.TestResult.ResultType
import org.podval.tools.test.taskdef.{Handling, Running, Selectors}
import org.podval.tools.util.Scala212Collections.{arrayAppend, arrayFind}
import sbt.testing.{Event, Selector}

final private class EventHandler(runTestClass: RunTestClass):
  // JUnit4 emits SUCCESS event for tests that were skipped because of a falsified assumption;
  // we suppress such events lest Gradle report two copies of a test - one skipped, one passed.
  private var skippedTests: Array[Selector] = Array.empty

  private def testResultProcessor: TestResultProcessorEx = runTestClass.testResultProcessor
  
  def handleEvent(event: Event): Unit =
    runTestClass.debug(
      s"""EventHandler.handleEvent
         |  className=${runTestClass.className}
         |  running=${runTestClass.running}
         |  event.fullyQualifiedName=${event.fullyQualifiedName}
         |  event.duration=${event.duration}
         |  event.selector=${event.selector}
         |  event.status=${event.status}
         |  event.throwable=${event.throwable}""", // a problem on Scala 2.12: .stripMargin
    )

    val endTime: Long = testResultProcessor.getCurrentTime
    val eventFor: Running = Running.forEvent(event.selector)
    val handling: Handling = Handling.forEvent(event.status, event.throwable)

    if runTestClass.running.isTest then
      // running individual test case (ScalaCheck packages test methods into nested NestedTest tasks).
      require(runTestClass.running.sameAs(eventFor))
      handling match
        case Handling.Failure(throwable) => testResultProcessor.failure(runTestClass.testId, throwable)
        case _ =>
    else
      // running suite
      // Events with overall results of suits are ignored; only events for individual test cases are processed:
      // - started/completed Gradle events are emitted in run();
      // - Gradle calculates the overall result from the outcomes of the individual tests.
      if
        eventFor.isTestAndNotForClass(runTestClass.className) &&
        arrayFind(skippedTests, Selectors.equal(_, eventFor.selector)).isEmpty
      then
        def startedThen(action: (TestResultProcessorEx, AnyRef) => Unit): Unit =
          val eventTestId: AnyRef = testResultProcessor.generateId()

          runTestClass.started(
            parentId = runTestClass.testId,
            testId = eventTestId,
            running = eventFor,
            startTime = endTime - event.duration
          )

          action(testResultProcessor, eventTestId)

        handling match
          case Handling.Failed =>
            // We get here when test framework does not bubble up an exception for a test failure,
            // which can happen even with my fixes for
            // JUnit4 for Scala.js (https://github.com/scala-js/scala-js/pull/5132),
            // JUnit4 for Scala Native (https://github.com/scala-native/scala-native/pull/4320),
            // and Weaver - TODO add a link.
            // Since Gradle requires an exception to accompany a test failure,
            // we make one up:
            startedThen(_.failure(_, new IllegalArgumentException(
              "FAKE THROWABLE TO REPLACE EXCEPTION THAT THE TEST FRAMEWORK DID NOT REPORT"
            )))

          case Handling.Failure(throwable) =>
            startedThen(_.failure(_, throwable))

          case Handling.Success =>
            startedThen(_.completed(_, endTime, ResultType.SUCCESS))

          case Handling.Skipped(hasThrowable) =>
            skippedTests = arrayAppend(skippedTests, eventFor.selector)
            if !runTestClass.running.isTests || hasThrowable || runTestClass.dryRun then
              startedThen(_.completed(_, endTime, ResultType.SKIPPED))
