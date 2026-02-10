package org.podval.tools.test.run

import org.gradle.api.tasks.testing.TestResult.ResultType
import org.podval.tools.util.Scala212Collections.{arrayAppend, arrayFind}
import sbt.testing.{Event, NestedSuiteSelector, NestedTestSelector, Selector, SuiteSelector}

/*
Note: something changed with Gradle 9.3.0: I did not have handleNestedClass() before, and:
 - tests in nested classes ended up attributed to the nested classes;
 - nesting classes were listed in the binary test report with 0 tests executed.

Now:

When I treat nested classes as *siblings* the main class:

  IDE's test tree shows only the Nested (not good)

  Binary test report lists Nesting classes as *methods*:

    3: org.podval.tools.test.ScalaTestNested failed=1 skipped=0
      9: success should pass resultType=SUCCESS
      10: failure should fail resultType=FAILURE
    2: org.podval.tools.test.JUnit4Nested failed=1 skipped=0
      5: success resultType=SUCCESS
      6: failure resultType=FAILURE
    1: Gradle Test Run :jvm:test failed=0 skipped=0
      3: org.podval.tools.test.JUnit4Nesting resultType=SUCCESS
      7: org.podval.tools.test.ScalaTestNesting resultType=SUCCESS

When I treat nested classes as *children* of the main class:

  IDE's test tree shows both the Nesting and the Nested (good).

  Binary test report does not list Nesting classes *at all*:

    2: org.podval.tools.test.ScalaTestNested failed=1 skipped=0
      9: success should pass resultType=SUCCESS
      10: failure should fail resultType=FAILURE
    1: org.podval.tools.test.JUnit4Nested failed=1 skipped=0
      5: success resultType=SUCCESS
      6: failure resultType=FAILURE

I have to adjust the test fixtures for the nested stuff to check
that there is no data for the nesting classes in the binary report
instead of checking that there is and the number of tests executed is zero...

TODO maybe there are some parameters I can set for report generation or reading?
 */
final private class EventHandler(runTestClass: RunTestClass):
  private def testResultProcessor: TestResultProcessorEx = runTestClass.testResultProcessor

  // JUnit4 emits SUCCESS event for tests that were skipped because of a falsified assumption;
  // we suppress such events lest Gradle report two copies of a test - one skipped, one passed.
  private var skippedTests: Array[Selector] = Array.empty

  private var nestedClasses: Array[(String, AnyRef)] = Array.empty

  private def isClass(testName: String): Boolean =
    (testName == runTestClass.className) ||
    arrayFind(nestedClasses, _._1 == testName).isDefined

  private def toSelectors(selector: Selector): Selectors = Selectors(selector) match
    case result@Selectors.Test(testSelector) =>
      val testName: String = testSelector.testName
      // JUnit4 emits overall class failure events with a `TestSelector`.
      if isClass(testName) then Selectors(SuiteSelector()) else
        // JUnit4 and its friends use TestSelector in place of NestedTestSelector
        // and stick the class name in front of the method name;
        // there is no chance to convince them to correct this ;)
        if !runTestClass.frameworkUsesTestSelectorAsNested then result else
          val lastDot: Int = testName.lastIndexOf('.')
          if lastDot == -1
          then result
          else Selectors(NestedTestSelector(
            testName.substring(0, lastDot),
            testName.substring(lastDot + 1)
          ))
    case result => result

  // When test belongs to a nested class:
  // - implicitly start that class
  // - with the nesting's parent as a parent (sibling to the nesting)
  // - set its test id as a parent for all tests from this class
  private def handleNestedClass(eventFor: Selectors): Option[AnyRef] = eventFor match
    case Selectors.NestedTest(nestedTestSelector) =>
      val nestedTestClassName: String = nestedTestSelector.suiteId
      if nestedTestClassName == runTestClass.className then None else Some:
        arrayFind(nestedClasses, _._1 == nestedTestClassName).map(_._2).getOrElse:
          val nestedClassTestId: AnyRef = testResultProcessor.generateId()
          nestedClasses = arrayAppend(nestedClasses, (nestedTestClassName, nestedClassTestId))
          runTestClass.started(
            parentId = runTestClass.testId,
            testId = nestedClassTestId,
            selectors = Selectors(NestedSuiteSelector(nestedTestClassName)),
            startTime = testResultProcessor.getCurrentTime
          )
          nestedClassTestId
    case _ => None

  def handleEvent(event: Event): Unit =
    runTestClass.debug(
      s"""EventHandler.handleEvent
         |  className=${runTestClass.className}
         |  selectors=${runTestClass.selectors}
         |  event.fullyQualifiedName=${event.fullyQualifiedName}
         |  event.duration=${event.duration}
         |  event.selector=${event.selector}
         |  event.status=${event.status}
         |  event.throwable=${event.throwable}""", // a problem on Scala 2.12: .stripMargin
    )

    val endTime: Long = testResultProcessor.getCurrentTime
    val result: Result = Result(event.status, event.throwable)
    val eventFor: Selectors = toSelectors(event.selector)

    if !runTestClass.selectors.isSuite then
      // running individual test case (ScalaCheck packages test methods into nested NestedTest tasks).
      require(Selectors.equal(runTestClass.selectors.selector, eventFor.selector))
      result match
        case Result.Failure(throwable) => testResultProcessor.failure(runTestClass.testId, throwable)
        case _ =>
    else
      // running suite
      // Events with overall results of suits are ignored; only events for individual test cases are processed:
      // - started/completed Gradle events are emitted in run();
      // - Gradle calculates the overall result from the outcomes of the individual tests.
      if
        !eventFor.isSuite &&
        arrayFind(skippedTests, Selectors.equal(_, eventFor.selector)).isEmpty
      then
        def startedThen(action: (TestResultProcessorEx, AnyRef) => Unit): Unit =
          val nestedClassTestId: Option[AnyRef] = handleNestedClass(eventFor)
          val eventTestId: AnyRef = testResultProcessor.generateId()
          runTestClass.started(
            parentId = nestedClassTestId.getOrElse(runTestClass.testId),
            testId = eventTestId,
            selectors = eventFor,
            startTime = endTime - event.duration
          )
          action(testResultProcessor, eventTestId)

        result match
          case Result.Failed =>
            // We get here when test framework does not bubble up an exception for a test failure,
            // which can happen even with my fixes for
            // JUnit4 for Scala.js (https://github.com/scala-js/scala-js/pull/5132),
            // JUnit4 for Scala Native (https://github.com/scala-native/scala-native/pull/4320),
            // and Weaver (https://github.com/typelevel/weaver-test/pull/183).
            // Since Gradle requires an exception to accompany a test failure,
            // we make one up:
            startedThen(_.failure(_, new IllegalArgumentException(
              "FAKE THROWABLE TO REPLACE EXCEPTION THAT THE TEST FRAMEWORK DID NOT REPORT"
            )))

          case Result.Failure(throwable) =>
            startedThen(_.failure(_, throwable))

          case Result.Success =>
            startedThen(_.completed(_, ResultType.SUCCESS))

          case Result.Skipped(hasThrowable) =>
            skippedTests = arrayAppend(skippedTests, eventFor.selector)
            if !runTestClass.selectors.isTests || hasThrowable || runTestClass.dryRun then
              startedThen(_.completed(_, ResultType.SKIPPED))

