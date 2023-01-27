package org.podval.tools.test

import org.gradle.api.internal.tasks.testing.{DefaultTestFailure, DefaultTestFailureDetails, TestClassRunInfo, TestResultProcessor}
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.testing.TestFailure
import org.gradle.api.tasks.testing.TestResult.ResultType
import org.gradle.internal.id.{CompositeIdGenerator, IdGenerator, LongIdGenerator}
import org.gradle.internal.time.Clock
import org.opentorah.build.Gradle
import org.podval.tools.test.framework.FrameworkDescriptor
import sbt.testing.{Event, EventHandler, Framework, Runner, Status, Task, TaskDef}
import java.io.File
import java.net.URLClassLoader
import scala.util.control.NonFatal
import TestResultProcessorEx.*
import java.lang.reflect.Field

final class TestClassProcessor(
  groupByFramework: Boolean,
  isForked: Boolean,
  testClassPath: Array[File],
  runningInIntelliJIdea: Boolean,
  testTagsFilter: TestTagsFilter,
  clock: Clock
) extends org.gradle.api.internal.tasks.testing.TestClassProcessor:

  // TODO we should not need to carry testClassPath all the way here and can just use own classloader - mimonafshach:
  // - if we are running in Node, testClassLoader is ignored;
  // - if we are not forked, TestTaskScala.loadFrameworks() already added it to the classpath on which we are running.
  // - if we are forked, ForkingTestClassProcessor added it to the applicationClassPath on which we are running (?) - but no...
  private val testClassLoader: ClassLoader =
    if isForked
    then URLClassLoader(testClassPath.map(_.toURI.toURL))
    else getClass.getClassLoader

  import TestClassProcessor.FrameworkRun

  private var testResultProcessorOpt: Option[TestResultProcessor] = None
  private def testResultProcessor: TestResultProcessor = testResultProcessorOpt.get

  override def startProcessing(testResultProcessor: TestResultProcessor): Unit =
    testResultProcessorOpt = Some(testResultProcessor)

  private var frameworksRuns: Seq[FrameworkRun] = Seq.empty

  private def getRunner(framework: Framework): Runner = synchronized {
    frameworksRuns.find(_.framework eq framework).map(_.runner).getOrElse {
      val frameworkDescriptor: FrameworkDescriptor = FrameworkDescriptor.forFramework(framework)

      val args: Array[String] = frameworkDescriptor.args(
        testTagsFilter = testTagsFilter
      )

      val runner: Runner = framework.runner(
        args,
        Array.empty,
        testClassLoader
      )

      val run: FrameworkRun = FrameworkRun(
        framework = framework,
        runner = runner
      )

      frameworksRuns = frameworksRuns :+ run

      runner
    }
  }

  /**
   * Stops any pending or asynchronous processing immediately.
   * Any test class assigned to this processor, but not yet run will not have results in the output.
   */
  override def stopNow(): Unit = stop()

  /**
   * Completes any pending or asynchronous processing. Blocks until all processing is complete.
   */
  override def stop(): Unit =
    for frameworkRun: FrameworkRun <- frameworksRuns do
      val summary: String = frameworkRun.runner.done()
      testResultProcessor.log(
        test = RootTest.forFramework(frameworkRun.framework, groupByFramework),
        message = s"${frameworkRun.framework.name}:\n$summary",
        logLevel = LogLevel.INFO
      )

  override def processTestClass(testClassRunInfo: TestClassRunInfo): Unit =
    val test: TaskDefTest = testClassRunInfo.asInstanceOf[TaskDefTest]

    val tasks: Array[Task] = getRunner(test.framework).tasks(Array(test.taskDef))
    if tasks.isEmpty then fail(test, "test rejected by the framework") else
    if tasks.length > 1 then fail(test, s"""multi-task test: ${tasks.mkString("Array(", ", ", ")")}""") else
      run(
        test = test,
        task = tasks.head
      )

  private def fail(test: TaskDefTest, message: String): Unit =
    testResultProcessor.started(
      test = test,
      startTime = clock.getCurrentTime
    )
    testResultProcessor.failed(
      test = test,
      testFailure = DefaultTestFailure(
        null,
        DefaultTestFailureDetails(
          message,
          test.getTestClassName,
          null,
          false,
          null,
          null
        ),
        null
      )
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
      if eventIsAboutNestedTest then testResultProcessor.started(
        test = eventTest,
        startTime = endTime - event.duration
      )

      if event.throwable.isDefined then testResultProcessor.failed(
        test = eventTest,
        testFailure = TestClassProcessor.throwableToTestFailure(event.throwable.get)
      )

      testResultProcessor.completed(
        test = eventTest,
        endTime = endTime,
        resultType = TestClassProcessor.fromStatus(event.status)
      )

      if !eventIsAboutNestedTest then testCompleted = true

    val startTime: Long = clock.getCurrentTime
    testResultProcessor.started(
      test = test,
      startTime = startTime
    )

    // skipped test
    if !testTagsFilter.allowed(task.tags) then
      testResultProcessor.completed(
        test = test,
        endTime = startTime,
        resultType = ResultType.SKIPPED
      )
    else
      val testLogger: TestLogger = TestLogger(
        testResultProcessor = testResultProcessor,
        test = test,
        useColours = !runningInIntelliJIdea
      )

      try
        val nestedTasks: Seq[Task] =
          try
            task.execute(
              eventHandler,
              Array(testLogger)
            ).toSeq
          catch case throwable@(_: NoClassDefFoundError | _: IllegalAccessError | NonFatal(_)) =>
            testResultProcessor.failed(
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
        if !testCompleted then testResultProcessor.completed(
          test = test,
          endTime = clock.getCurrentTime
        )

object TestClassProcessor:

  private class FrameworkRun(
    val framework: Framework,
    val runner: Runner
  )

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
