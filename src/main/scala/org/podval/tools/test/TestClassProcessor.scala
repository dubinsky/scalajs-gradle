package org.podval.tools.test

import org.gradle.api.internal.tasks.testing.{DefaultTestOutputEvent, TestClassRunInfo, TestResultProcessor}
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.testing.{TestFailure, TestOutputEvent}
import org.gradle.api.tasks.testing.TestResult.ResultType
import org.gradle.internal.id.{CompositeIdGenerator, IdGenerator, LongIdGenerator}
import org.gradle.internal.time.Clock
import sbt.testing.{Event, EventHandler, Status, Task, TaskDef}
import scala.util.control.NonFatal
import java.lang.reflect.Field

final class TestClassProcessor(
  frameworkRuns: FrameworkRuns,
  groupByFramework: Boolean,
  runningInIntelliJIdea: Boolean,
  testTagsFilter: TestTagsFilter,
  clock: Clock,
  logLevelEnabled: LogLevel
) extends org.gradle.api.internal.tasks.testing.TestClassProcessor:

  private var testResultProcessorOpt: Option[TestResultProcessor] = None
  private def testResultProcessor: TestResultProcessor = testResultProcessorOpt.get

  override def startProcessing(testResultProcessor: TestResultProcessor): Unit =
    testResultProcessorOpt = Some(testResultProcessor)

  /**
   * Stops any pending or asynchronous processing immediately.
   * Any test class assigned to this processor, but not yet run will not have results in the output.
   */
  // TODO implement
  override def stopNow(): Unit = stop()

  /**
   * Completes any pending or asynchronous processing. Blocks until all processing is complete.
   */
  override def stop(): Unit =
    for frameworkRun: FrameworkRuns.Run <- frameworkRuns.getRuns do
      val summary: String = frameworkRun.runner.done()
      output(
        test = RootTest.forFramework(frameworkRun.framework, groupByFramework),
        message = s"${frameworkRun.framework.name} summary:\n$summary",
        logLevel = LogLevel.INFO //.LIFECYCLE
      )

  override def processTestClass(testClassRunInfo: TestClassRunInfo): Unit =
    val test: TaskDefTest = testClassRunInfo.asInstanceOf[TaskDefTest]
    val tasks: Array[Task] = frameworkRuns.getRunner(test.framework).tasks(Array(test.taskDef))
    // For test classes that do not have tests, empty tasks array is returned;
    // there is no need nor point to report such an occurrence:
    // nothing shows up un the Idea's test tree.
    // I never saw ScalaTest (the only framework I use) return more than one task,
    // so I do not yet know how would that be reported by Gradle/Idea.
    for task: Task <- tasks do run(
      test = test,
      task = task
    )

  private def run(
    test: TaskDefTest,
    task: Task
  ): Unit =
    var testCompleted: Boolean = false

    val idGenerator: IdGenerator[?] = CompositeIdGenerator(test.getId, new LongIdGenerator)

    val eventHandler: EventHandler = (event: Event) =>
      val eventTest: TaskDefTest = test.thisOrNested(
        mustBeNested = false,
        idGenerator = idGenerator,
        taskDef = TaskDef(
          event.fullyQualifiedName,
          event.fingerprint,
          false,
          Array(event.selector)
        )
      )

      val eventIsAboutNestedTest: Boolean = eventTest ne test

      val endTime: Long = clock.getCurrentTime

      // Note: for implied eventTest we reconstruct the 'started' event
      if eventIsAboutNestedTest then eventTest.started(endTime - event.duration, testResultProcessor)

      if event.throwable.isDefined then failed(
        test = eventTest,
        testFailure = TestClassProcessor.throwableToTestFailure(event.throwable.get)
      )

      eventTest.completed(endTime, TestClassProcessor.fromStatus(event.status), testResultProcessor)

      if !eventIsAboutNestedTest then testCompleted = true

    val startTime: Long = clock.getCurrentTime
    test.started(startTime, testResultProcessor)

    // skipped test
    if !testTagsFilter.allowed(task.tags) then
      test.completed(startTime, ResultType.SKIPPED, testResultProcessor)
    else
      try
        val nestedTasks: Seq[Task] =
          try
            task.execute(
              eventHandler,
              Array(testLogger(test))
            ).toSeq
          catch case throwable@(_: NoClassDefFoundError | _: IllegalAccessError | NonFatal(_)) =>
            failed(
              test = test,
              testFailure = TestFailure.fromTestFrameworkFailure(throwable)
            )
            Seq.empty

        for (task: Task, index: Int) <- nestedTasks.zipWithIndex do run(
          task = task,
          test = test.thisOrNested(
            mustBeNested = true,
            idGenerator = idGenerator,
            taskDef = task.taskDef // TODO test.getClassName + "-" + index?
          )
        )
      finally
        if !testCompleted then test.completed(clock.getCurrentTime, ResultType.SUCCESS, testResultProcessor)

  private def testLogger(test: TaskDefTest): sbt.testing.Logger = new sbt.testing.Logger:
    private def log(logLevel: LogLevel, message: String): Unit = output(
      test = test,
      message = message,
      logLevel = logLevel
    )

    // TODO I do not see any issues with the colors on in Idea when the output is delivered through
    // the proper channels, so there seems to be no need for "!runningInIntelliJIdea" here
    // when running ScalaTest - but MUnit's and UTest's output gets garbled with the color escape sequences
    // even *with* the flag...
    override def ansiCodesSupported: Boolean = true
    override def error(message: String): Unit = log(LogLevel.ERROR, message)
    override def warn(message: String): Unit = log(LogLevel.WARN, message)
    override def info(message: String): Unit = log(LogLevel.INFO, message)
    override def debug(message: String): Unit = log(LogLevel.DEBUG, message)
    override def trace(throwable: Throwable): Unit = failed(test = test, TestFailure.fromTestFrameworkFailure(throwable))

  private given CanEqual[LogLevel, LogLevel] = CanEqual.derived
  private def output(
    test: Test,
    message: String,
    logLevel: LogLevel
  ): Unit =
    val isEnabled: Boolean = !runningInIntelliJIdea || (logLevel.ordinal >= logLevelEnabled.ordinal)
    if isEnabled then testResultProcessor.output(
      test.getId,
      DefaultTestOutputEvent(
        if (logLevel == LogLevel.ERROR) || (logLevel == LogLevel.WARN)
        then TestOutputEvent.Destination.StdErr
        else TestOutputEvent.Destination.StdOut,
        s"$message\n"
      )
    )

  private def failed(
    test: Test,
    testFailure: TestFailure
  ): Unit = testResultProcessor.failure(
    test.getId,
    testFailure
  )

object TestClassProcessor:

  private given CanEqual[Status, Status] = CanEqual.derived
  private def fromStatus(status: Status): ResultType = status match
    case Status.Success  => ResultType.SUCCESS
    case Status.Error    => ResultType.FAILURE
    case Status.Failure  => ResultType.FAILURE
    case Status.Skipped  => ResultType.SKIPPED
    case Status.Ignored  => ResultType.SKIPPED
    case Status.Canceled => ResultType.SKIPPED
    case Status.Pending  => ResultType.SKIPPED

  // Note: translated from org.gradle.api.internal.tasks.testing.junit.JUnitTestEventAdapter
  // TODO add handling of exceptions thrown by other frameworks - and use the fields/methods only dynamically!
  // According to https://junit.org/junit4/javadoc/latest/overview-tree.html, JUnit assertion failures can be expressed with the following exceptions:
  // - java.lang.AssertionError: general assertion errors, i.e. test code contains assert statements
  // - org.junit.ComparisonFailure: when assertEquals (and similar assertion) fails; test code can throw it directly
  // - junit.framework.ComparisonFailure: for older JUnit tests using JUnit 3.x fixtures
  // All assertion errors are subclasses of the AssertionError class. If the received failure is not an instance of AssertionError then it is categorized as a framework failure.
  private def throwableToTestFailure(throwable: Throwable) = throwable match
//    case comparisonFailure: org.junit.ComparisonFailure => TestFailure.fromTestAssertionFailure(comparisonFailure, comparisonFailure.getExpected, comparisonFailure.getActual)
//    case comparisonFailure: junit.framework.ComparisonFailure => TestFailure.fromTestAssertionFailure(comparisonFailure, getValueOfStringField("fExpected", comparisonFailure), getValueOfStringField("fActual", comparisonFailure))
    case assertionError: AssertionError => TestFailure.fromTestAssertionFailure(assertionError, null, null)
    case error => TestFailure.fromTestFrameworkFailure(error)

  private def getValueOfStringField(name: String, comparisonFailure: junit.framework.ComparisonFailure): String =
    try
      val f: Field = comparisonFailure.getClass.getDeclaredField(name)
      f.setAccessible(true)
      f.get(comparisonFailure).asInstanceOf[String]
    catch
      case _: Exception => null
