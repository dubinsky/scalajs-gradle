package org.podval.tools.scalajs

import org.gradle.api.internal.tasks.testing.{DefaultTestOutputEvent, TestCompleteEvent, TestDescriptorInternal,
  TestStartEvent}
import org.gradle.api.internal.tasks.testing.results.TestListenerInternal
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.testing.{TestDescriptor, TestListener, TestOutputEvent, TestOutputListener, TestResult}
import org.gradle.api.tasks.testing.TestResult.ResultType
import sbt.testing.{TestSelector, Logger as TLogger} // SBT: test-interface

/*
  This is the glue between SBT testing and Gradle testing.
  Since Gradle's Test task uses org.gradle.internal.event.ListenerBroadcast,
  only one instance of each type of listener is used.
*/
// TODO intercept test output (from the Piped stream used by SBT testing do)?
// TODO reporting:
// - how to make ScalaTest produce the report?
// - TestReporter, TestResultProcessor
abstract class TestListeners(
  testListener: TestListener,
  testOutputListener: TestOutputListener,
  testListenerInternal: TestListenerInternal,
  sourceMapper: SourceMapper
):
  private given CanEqual[ResultType, ResultType] = CanEqual.derived

// Note: based on sbt.TestFramework from org.scala-sbt.testing

  /** called for each class or equivalent grouping */
  final def startGroup(test: TestDescriptorInternal, startTime: Long): Unit =
    testListener.beforeSuite(test)
    testListenerInternal.started(test, TestStartEvent(startTime))

    val summary: String = Colours.withColour(Colours.green, s"${test.getName}: Run starting")
    startGroup(summary)

  def startGroup(summary: String): Unit
  /** called for each test method or equivalent */
  final def testEvent(event: TestEvent): Unit =
    // TODO there SHOULD be a Gradle equivalent!

    // TODO could be NestedSuiteSelector...
    val testName: String = event.selector.asInstanceOf[TestSelector].testName
    val summary: String =
      if event.status == ResultType.SUCCESS then Colours.withColour(Colours.green, s"- $testName") else
        Colours.withColour(Colours.red, s"- $testName *** FAILED ***\n") +
        event.throwable.fold("") { (throwable: Throwable) =>
          val message: String = Option(throwable.getMessage)
            .getOrElse(s"$throwable was thrown") // TODO look at throwable.getCause?
          val source: String = sourceMapper.stackTraceMessage(throwable)
          Colours.withColour(Colours.red, s"  $message ($source)")
        }
    testEvent(summary)

  def testEvent(summary: String): Unit

  final def endGroup(test: TestDescriptorInternal, results: Map[String, TestResult]): Unit =
    val result: TestResult = Tests.combineTestResults(results.toSeq)
    testListener.afterSuite(test, result)
    testListenerInternal.completed(test, result, TestCompleteEvent(result.getEndTime, result.getResultType))

    val summary: String =
      Colours.withColour(Colours.cyan, s"${test.getName}: Run completed in ${result.getEndTime-result.getStartTime} milliseconds.\n") +
      Colours.withColour(Colours.cyan, s"Total number of tests run: ${result.getTestCount}\n") +
      Colours.withColour(Colours.cyan, s"Suites: succeeded ${results.count(_._2.getResultType == ResultType.SUCCESS)}, failed ${results.count(_._2.getResultType == ResultType.FAILURE)}\n") +
      Colours.withColour(Colours.cyan, s"Tests: succeeded ${result.getSuccessfulTestCount}, failed ${result.getFailedTestCount}, skipped ${result.getSkippedTestCount}\n") +
      (if result.getResultType == ResultType.SUCCESS
      then Colours.withColour(Colours.green, "All tests passed")
      else Colours.withColour(Colours.red  , s"*** ${result.getFailedTestCount} TESTS FAILED ***"))
    endGroup(summary)

  def endGroup(summary: String): Unit

  final def output(test: TestDescriptorInternal, message: String, isError: Boolean): Unit =
    val event: TestOutputEvent = DefaultTestOutputEvent(
      if isError then TestOutputEvent.Destination.StdErr else TestOutputEvent.Destination.StdOut,
      message
    )
    testOutputListener.onOutput(test, event)
    testListenerInternal.output(test, event)

  // Note: output is reconstructed in testEvent()
  final def contentLoggers(test: TestDescriptorInternal): Array[TLogger] = Array.empty

  // Note: I think there is no equivalent for this in Gradle...
  final def flushContentLoggers(test: TestDescriptorInternal): Unit = ()

object TestListeners:

  def apply(sourceMapper: SourceMapper, gradleLogger: Logger): TestListeners =
    def log(message: String): Unit = gradleLogger.lifecycle(message)

    new TestListeners(
      sourceMapper = sourceMapper,
      testListener = new TestListener:
        def beforeSuite(test: TestDescriptor): Unit = ()
        def afterSuite(test: TestDescriptor, result: TestResult): Unit = ()
        // Note: we do not get those from SBT
        def beforeTest(test : TestDescriptor): Unit = ()
        def afterTest(test : TestDescriptor, result: TestResult): Unit = ()
      ,
      testOutputListener = new TestOutputListener:
        // Fired when during test execution anything is printed to standard output or error
        override def onOutput(test: TestDescriptor, event: TestOutputEvent): Unit =
          log(s"TestListeners.output($test, ${event.getDestination}, ${event.getMessage})")
      ,
      testListenerInternal = new TestListenerInternal:
        override def started  (test: TestDescriptorInternal, event: TestStartEvent): Unit = ()
        override def completed(test: TestDescriptorInternal, result: TestResult, event: TestCompleteEvent): Unit = ()
        override def output   (test: TestDescriptorInternal, event: TestOutputEvent): Unit = ()
    ):
      override def startGroup(summary: String): Unit = log(summary)
      override def testEvent (summary: String): Unit = log(summary)
      override def endGroup  (summary: String): Unit = log(summary)

//      override def contentLoggers(test: TestDescriptorInternal): Array[TLogger] = Array(new TLogger:
//        private def output(level: String, message: String): Unit = log(s"${print(test)} - $level: $message")
//        override def ansiCodesSupported: Boolean = true
//        override def error(msg: String): Unit = output("error", msg)
//        override def warn (msg: String): Unit = output("warn ", msg)
//        override def info (msg: String): Unit = output("info ", msg)
//        override def debug(msg: String): Unit = output("debug", msg)
//        override def trace(t: Throwable): Unit = output("error", t.getMessage)
//      )
