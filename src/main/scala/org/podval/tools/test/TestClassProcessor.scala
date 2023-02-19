package org.podval.tools.test

import org.gradle.api.internal.tasks.testing.{DefaultTestMethodDescriptor, DefaultTestOutputEvent, TestClassRunInfo,
  TestCompleteEvent, TestDescriptorInternal, TestResultProcessor, TestStartEvent}
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.testing.{TestFailure, TestOutputEvent}
import org.gradle.api.tasks.testing.TestResult.ResultType
import org.gradle.internal.id.{CompositeIdGenerator, LongIdGenerator}
import org.gradle.internal.time.Clock
import org.podval.tools.test.serializer.TaskDefSerializer
import sbt.testing.{Event, EventHandler, Selector, Status, Task, TaskDef}
import scala.util.control.NonFatal
import java.lang.reflect.Field

final class TestClassProcessor(
  frameworkRuns: FrameworkRuns,
  runningInIntelliJIdea: Boolean,
  testTagsFilter: TestTagsFilter,
  clock: Clock,
  logLevelEnabled: LogLevel,
  rootTestSuiteId: AnyRef
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
        testId = rootTestSuiteId,
        message = s"${frameworkRun.framework.name} summary:\n$summary",
        // TODO if this is LIFECYCLE, everything hangs even though rootTestSuiteId is supplied - because it is a String?!
        // how about making it nullable, use the null - and remove the rootTestSuiteId?
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
    for task: Task <- tasks do run(null, test, task)

  private def run(
    parentId: AnyRef,
    test: TaskDefTest,
    task: Task
  ): Unit =
    var isTestCompleted: Boolean = false

    val idGenerator: CompositeIdGenerator = CompositeIdGenerator(test.id, new LongIdGenerator)

    val startTime: Long = clock.getCurrentTime
    testResultProcessor.started(test.toTestDescriptorInternal, TestStartEvent(startTime, parentId))

    // skipped test
    if !testTagsFilter.allowed(task.tags) then
      testResultProcessor.completed(test.id, TestCompleteEvent(startTime, ResultType.SKIPPED))
    else
      try
        val nestedTasks: Seq[Task] =
          try
            task.execute(
              (event: Event) =>
                if isTestCompleted then throw IllegalStateException(s"Received event for a completed test $test")
                isTestCompleted = !handleEvent(test, event, idGenerator),
              Array(testLogger(test))
            ).toSeq
          catch case throwable@(_: NoClassDefFoundError | _: IllegalAccessError | NonFatal(_)) =>
            testResultProcessor.failure(test.id, TestFailure.fromTestFrameworkFailure(throwable))
            Seq.empty

        for task: Task <- nestedTasks do
          val taskDef: TaskDef = task.taskDef
          test.verifyCanHaveNestedTest(taskDef)
          val nestedTest: TaskDefTest = TaskDefTest(
            id = idGenerator.generateId,
            framework = test.framework,
            taskDef = taskDef
          )
          run(test.id, nestedTest, task)
      finally
        if !isTestCompleted then
          testResultProcessor.completed(test.id, TestCompleteEvent(clock.getCurrentTime, ResultType.SUCCESS))

  private def handleEvent(
    test: TaskDefTest,
    event: Event,
    idGenerator: CompositeIdGenerator
  ): Boolean =
    val endTime: Long = clock.getCurrentTime
    val selector: Selector = event.selector
    val className: String = event.fullyQualifiedName

    val taskDef: TaskDef = TaskDef(
      className,
      event.fingerprint,
      false,
      Array(selector)
    )

    val isNestedTest: Boolean = !TaskDefSerializer.equal(test.taskDef, taskDef)

    val eventTest: TestDescriptorInternal =
      if !isNestedTest then test.toTestDescriptorInternal else
        test.verifyCanHaveNestedTest(taskDef)
        val nestedTest: DefaultTestMethodDescriptor = DefaultTestMethodDescriptor(
          idGenerator.generateId(),
          className,
          TaskDefTest.methodName(selector).get
        )
        // Note: for implied eventTest we reconstruct the 'started' event
        testResultProcessor.started(nestedTest, TestStartEvent(endTime - event.duration, test.id))
        nestedTest

    if event.throwable.isDefined then
      testResultProcessor.failure(eventTest.getId, TestClassProcessor.throwableToTestFailure(event.throwable.get))

    testResultProcessor.completed(eventTest.getId, TestCompleteEvent(endTime, TestClassProcessor.fromStatus(event.status)))

    isNestedTest

  private def testLogger(test: TaskDefTest): sbt.testing.Logger = new sbt.testing.Logger:
    private def log(logLevel: LogLevel, message: String): Unit = output(
      testId = test.id,
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
    override def trace(throwable: Throwable): Unit =
      testResultProcessor.failure(test.id, TestFailure.fromTestFrameworkFailure(throwable))

  private given CanEqual[LogLevel, LogLevel] = CanEqual.derived
  private def output(
    testId: AnyRef,
    message: String,
    logLevel: LogLevel
  ): Unit =
    val isEnabled: Boolean =
      (!runningInIntelliJIdea && testId != null && !testId.isInstanceOf[String]) ||
      (logLevel.ordinal >= logLevelEnabled.ordinal)
    if isEnabled then testResultProcessor.output(
      testId,
      DefaultTestOutputEvent(
        if (logLevel == LogLevel.ERROR) || (logLevel == LogLevel.WARN)
        then TestOutputEvent.Destination.StdErr
        else TestOutputEvent.Destination.StdOut,
        s"$message\n"
      )
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
