package org.podval.tools.test.run

import org.gradle.api.internal.tasks.testing.{DefaultTestClassDescriptor, DefaultTestMethodDescriptor,
  DefaultTestOutputEvent, TestClassProcessor, TestClassRunInfo, TestCompleteEvent, TestResultProcessor, TestStartEvent}
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.testing.TestOutputEvent
import org.gradle.api.tasks.testing.TestResult.ResultType
import org.gradle.internal.id.IdGenerator
import org.gradle.internal.id.CompositeIdGenerator.CompositeId
import org.gradle.internal.time.Clock
import org.podval.tools.test.exception.ExceptionConverter
import org.podval.tools.test.taskdef.{FrameworkProvider, Selectors, TaskDefs, TestClassRun}
import org.podval.tools.util.Scala212Collections.{arrayAppend, arrayFind, arrayForEach}
import sbt.testing.{Event, Logger, Runner, Selector, Task, TaskDef}
import scala.util.control.NonFatal

object RunTestClassProcessor:
  val rootTestSuiteIdPlaceholder: CompositeId = CompositeId(0L, 0L)

final class RunTestClassProcessor(
  includeTags: Array[String],
  excludeTags: Array[String],
  runningInIntelliJIdea: Boolean, // TODO do I even need this?
  logLevelEnabled: LogLevel,
  dryRun: Boolean,
  idGenerator: IdGenerator[?],
  clock: Clock
) extends TestClassProcessor:

  private var testResultProcessorOpt: Option[TestResultProcessor] = None
  private def testResultProcessor: TestResultProcessor = testResultProcessorOpt.get

  override def startProcessing(testResultProcessor: TestResultProcessor): Unit =
    testResultProcessorOpt = Some(testResultProcessor)

  private def started(
    parentId: AnyRef,
    testId: AnyRef,
    testClassName: String,
    testName: Option[String],
    startTime: Long
  ): Unit = testResultProcessor.started(
    testName match
      case None           => DefaultTestClassDescriptor (testId, testClassName)
      case Some(testName) => DefaultTestMethodDescriptor(testId, testClassName, testName),
    TestStartEvent(startTime, parentId)
  )

  private def completed(
    testId: AnyRef,
    endTime: Long,
    result: ResultType
  ): Unit = testResultProcessor.completed(
    testId,
    TestCompleteEvent(endTime, result)
  )

  private def failure(
    testId: AnyRef,
    throwable: Throwable
  ): Unit = testResultProcessor.failure(
    testId,
    ExceptionConverter.exceptionConverter(throwable.getClass.getName).toTestFailure(throwable)
  )

  private def output(
    message: String,
    logLevel: LogLevel,
    testId: AnyRef = RunTestClassProcessor.rootTestSuiteIdPlaceholder
  ): Unit =
    given CanEqual[LogLevel, LogLevel] = CanEqual.derived
    if logLevel.ordinal >= logLevelEnabled.ordinal then testResultProcessor.output(
      testId,
      DefaultTestOutputEvent(
        clock.getCurrentTime,
        if (logLevel == LogLevel.ERROR) || (logLevel == LogLevel.WARN)
        then TestOutputEvent.Destination.StdErr
        else TestOutputEvent.Destination.StdOut,
        s"$message\n"
      )
    )

  private def testLogger(testId: AnyRef): Logger = new Logger:
    private def log(logLevel: LogLevel, message: String): Unit = output(message, logLevel, testId)
    override def ansiCodesSupported: Boolean = true // TODO !runningInIntelliJIdea?
    override def error(message: String): Unit = log(LogLevel.ERROR, message)
    override def warn (message: String): Unit = log(LogLevel.WARN , message)
    override def info (message: String): Unit = log(LogLevel.INFO , message)
    override def debug(message: String): Unit = log(LogLevel.DEBUG, message)
    override def trace(throwable: Throwable): Unit = failure(testId, throwable)

  private var runners: Array[(String, Runner)] = Array.empty
  private def getRunner(frameworkProvider: FrameworkProvider): Runner = synchronized:
    val frameworkName: String = frameworkProvider.frameworkName
    arrayFind(runners, _._1 == frameworkName).map(_._2).getOrElse:
      val runner: Runner = frameworkProvider.makeRunner(
        includeTags,
        excludeTags
      )
      runners = arrayAppend(runners, (frameworkName, runner))
      runner

  override def stop(): Unit = arrayForEach(runners, (frameworkName: String, runner: Runner) =>
    output(message = s"RunTestClassProcessor $frameworkName summary:\n${runner.done}", LogLevel.INFO)
  )

  private var stoppedNow: Boolean = false
  override def stopNow(): Unit =
    stoppedNow = true
    stop()

  def processTestClass(testClassRunInfo: TestClassRunInfo): Unit = if !stoppedNow then
    val testClassRun: TestClassRun = testClassRunInfo.asInstanceOf[TestClassRun]

    val running: Running = Running.forTestClassRun(testClassRun)

    val taskDef: TaskDef = TaskDef(
      testClassRun.getTestClassName,
      testClassRun.fingerprint,
      testClassRun.explicitlySpecified,
      running.selectors
    )

    val tasks: Array[Task] =
      if dryRun
      then Array(DryRunSbtTask(taskDef))
      else getRunner(testClassRun.frameworkProvider).tasks(Array(taskDef))

    require(tasks.length == 1)
    val task: Task = tasks(0)
    require(TaskDefs.equal(task.taskDef, taskDef))

    Run(
      className = taskDef.fullyQualifiedName,
      frameworkIncludesClassNameInTestName = testClassRun.frameworkProvider.frameworkDescriptor.includesClassNameInTestName
    ).run(
      parentId = null,
      running = running,
      task = task
    )

  final private class Run(
    className: String,
    frameworkIncludesClassNameInTestName: Boolean
  ):
    private def started(
      parentId: AnyRef,
      testId: AnyRef,
      running: Running,
      startTime: Long
    ): Unit =
      val runningEffective: Running =
        if !frameworkIncludesClassNameInTestName
        then running
        else running.reconstructNestedTestSelector
      
      RunTestClassProcessor.this.started(
        parentId,
        testId,
        runningEffective.suiteId.getOrElse(className),
        runningEffective.testName,
        startTime
      )

    def run(
      parentId: AnyRef,
      running: Running,
      task: Task
    ): Unit =
      output(s"RunTestClassProcessor.run(${TaskDefs.toString(task.taskDef)})", LogLevel.INFO)

      val startTime: Long = clock.getCurrentTime
      val testId: AnyRef = idGenerator.generateId()
      require(task.taskDef.fullyQualifiedName == className, s"${task.taskDef.fullyQualifiedName} should be $className")

      started(
        parentId = parentId,
        testId = testId,
        running = running,
        startTime = startTime
      )

      try
        val eventHandler: EventHandler = EventHandler(
          testId,
          running
        )

        val nestedTasks: Array[Task] = task.execute(
          eventHandler.handleEvent(_),
          Array(testLogger(testId))
        )

        arrayForEach(nestedTasks, (nestedTask: Task) =>
          output(s"RunTestClassProcessor: nested task ${TaskDefs.toString(task.taskDef)}", LogLevel.INFO)
          run(
            parentId = testId,
            running = running.forNestedTask(nestedTask),
            task = nestedTask
          )
        )

      catch case throwable@(_: NoClassDefFoundError | _: IllegalAccessError | NonFatal(_)) =>
        failure(testId, throwable)

      finally
        completed(
          testId = testId,
          endTime = clock.getCurrentTime,
          result = null
        )

    final private class EventHandler(
      testId: AnyRef,
      running: Running
    ):
      // JUnit4 emits SUCCESS event for tests that were skipped because of a falsified assumption;
      // we suppress such events lest Gradle report two copies of a test - one skipped, one passed.
      private var skipped: Array[Selector] = Array.empty

      def handleEvent(event: Event): Unit =
        val endTime: Long = clock.getCurrentTime
        val throwable: Option[Throwable] = if event.throwable.isEmpty then None else Some(event.throwable.get)
        val eventFor: Running = Running.forEvent(event)

        output(
          s"""RunTestClassProcessor.EventHandler.handleEvent:
             |className=$className
             |running=$running
             |event.fullyQualifiedName=${event.fullyQualifiedName}
             |event.selector=${event.selector}
             |event.status=${event.status}
             |event.throwable=$throwable""", // a problem on Scala 2.12: .stripMargin
          LogLevel.INFO
        )

        if running.isTest then
          // running individual test case (ScalaCheck packages test methods into nested NestedTest tasks).
          require(running.sameAs(eventFor))
          event.status.name match
            case "Error" | "Failure" => failure(testId, throwable.get)
            case _ =>
        else
          // running suite
          // Events with overall results of suits are ignored; only events for individual test cases are processed:
          // - started/completed Gradle events are emitted in run();
          // - Gradle calculates overall result from the outcomes of the individual tests.
          if
            eventFor.isTestAndNotForClass(className) &&
            arrayFind(skipped, Selectors.equal(_, eventFor.selector)).isEmpty
          then
            def reconstructStarted(): AnyRef =
              val eventTestId: AnyRef = idGenerator.generateId()

              started(
                parentId = testId,
                testId = eventTestId,
                running = eventFor,
                startTime = endTime - event.duration // TODO deal with negative durations?
              )

              eventTestId

            event.status.name match
              case "Success" =>
                completed(reconstructStarted(), endTime, ResultType.SUCCESS)

              case "Error" | "Failure" =>
                failure(reconstructStarted(), throwable.getOrElse(Exception("A FAKE EXCEPTION TO MAKE Gradle FEEL THE TEST FAILURE")))

              case _ =>
                skipped = arrayAppend(skipped, eventFor.selector)
                if !running.isTests || throwable.nonEmpty || dryRun then
                  completed(reconstructStarted(), endTime, ResultType.SKIPPED)
