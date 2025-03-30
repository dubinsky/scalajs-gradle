package org.podval.tools.test.run

import org.gradle.api.internal.tasks.testing.{DefaultTestClassDescriptor, DefaultTestMethodDescriptor,
  DefaultTestOutputEvent, TestClassProcessor, TestClassRunInfo, TestCompleteEvent, TestDescriptorInternal,
  TestResultProcessor, TestStartEvent}
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.testing.TestOutputEvent
import org.gradle.api.tasks.testing.TestResult.ResultType
import org.gradle.internal.id.IdGenerator
import org.gradle.internal.id.CompositeIdGenerator.CompositeId
import org.gradle.internal.time.Clock
import org.podval.tools.test.exception.ExceptionConverter
import org.podval.tools.test.taskdef.{Selectors, TaskDefs, TestClassRun}
import org.podval.tools.util.Scala212Collections.{arrayAppend, arrayFind, arrayForAll, arrayForEach, stripPrefix}
import sbt.testing.{Event, Logger, Runner, Selector, SuiteSelector, Task, TaskDef, TestSelector}
import scala.util.control.NonFatal

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
    className: String,
    selector: Selector,
    startTime: Long
  ): Unit =
    val testDescriptorInternal: TestDescriptorInternal = Selectors.testName(selector) match
      case None             => DefaultTestClassDescriptor (testId, className)
      case Some(methodName) => DefaultTestMethodDescriptor(testId, className, stripPrefix(methodName, className + "."))

    testResultProcessor.started(
      testDescriptorInternal,
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
  private def getRunner(testClassRun: TestClassRun): Runner = synchronized:
    val frameworkName: String = testClassRun.frameworkName
    arrayFind(runners, _._1 == frameworkName).map(_._2).getOrElse:
      val runner: Runner = testClassRun.makeRunner(
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
    val taskDef: TaskDef = testClassRun.taskDef

    val tasks: Array[Task] =
      if dryRun
      then Array(DryRunSbtTask(taskDef))
      else getRunner(testClassRun).tasks(Array(taskDef))

    require(tasks.length == 1)
    val task: Task = tasks(0)
    require(TaskDefs.equal(task.taskDef, taskDef))

    // see TestFilterMatch
    val selectors: Array[Selector] = taskDef.selectors
    val isAllTests: Boolean = arrayForAll(selectors, Selectors.isTestFromTestFilterMatch)
    if !isAllTests then
      require(selectors.length == 1, "If not all selectors are tests, there can only be one!")
      val selector: Selector = selectors(0)
      require(Selectors.equal(selector, SuiteSelector()), s"If not all selectors are tests, there can only be SuiteSelector, not $selector!")
    
    run(
      parentId = null,
      selector = if isAllTests then SuiteSelector() else selectors(0),
      task = task
    )

  private def run(
    parentId: AnyRef,
    selector: Selector,
    task: Task
  ): Unit =
    output(s"RunTestClassProcessor.run(${RunTestClassProcessor.toString(task)})", LogLevel.INFO)

    val startTime: Long = clock.getCurrentTime
    val testId: AnyRef = idGenerator.generateId()
    val className: String = task.taskDef.fullyQualifiedName
    
    started(
      parentId,
      testId,
      className,
      selector,
      startTime
    )
    
    try
      output(s"RunTestClassProcessor: Task(${RunTestClassProcessor.toString(task)}).execute()", LogLevel.INFO)
      
      val eventHandler: EventHandler = EventHandler(
        testId,
        className,
        selector,
        isAllTests = arrayForAll(task.taskDef.selectors, Selectors.isTest)
      )
      
      val nestedTasks: Array[Task] = task.execute(
        eventHandler.handleEvent(_),
        Array(testLogger(testId))
      )
      
      arrayForEach(nestedTasks, (nestedTask: Task) => runNestedTask(
        parentSelector = selector,
        parentId = testId,
        task = nestedTask
      ))
      
    catch case throwable@(_: NoClassDefFoundError | _: IllegalAccessError | NonFatal(_)) =>
      failure(testId, throwable)
      
    finally
      completed(
        testId = testId,
        endTime = clock.getCurrentTime,
        result = null
      )

  private def runNestedTask(
    parentSelector: Selector,
    parentId: AnyRef,
    task: Task
  ): Unit =
    output(s"RunTestClassProcessor: nested task ${RunTestClassProcessor.toString(task)}", LogLevel.INFO)
    
    require(Selectors.canHaveNested(parentSelector), s"$parentSelector can not have nested tests!")
    val selectors: Array[Selector] = task.taskDef.selectors

    require(selectors.length == 1, "Only one selector can be nested!")
    val selector: Selector = selectors(0)
    
    require(Selectors.canBeNested(selector), s"$selector can not be nested")
    
    run(
      parentId = parentId,
      selector = selector,
      task = task
    )

  final private class EventHandler(
    testId: AnyRef,
    // TODO      val className: String = event.fullyQualifiedName
    className: String,
    selector: Selector,
    isAllTests: Boolean
  ):
    // Are we running a suite or an individual test case?
    private val isRunningSuite: Boolean = Selectors.isRunningSuite(selector)
    
    // TODO if isTests then require(isRunningSuite)

    // JUnit4 emits SUCCESS event for tests that were skipped because of a falsified assumption;
    // we suppress such events lest Gradle report two copies of a test - one skipped, one passed.
    private var skipped: Array[Selector] = Array.empty

    def handleEvent(event: Event): Unit =
      val endTime: Long = clock.getCurrentTime
      val throwable: Option[Throwable] = if event.throwable.isEmpty then None else Some(event.throwable.get)

      val isEventForTest: Boolean = Selectors.isEventForTest(event.selector)

      output(
        s"""RunTestClassProcessor.EventHandler.handleEvent:
           |className=$className
           |selector=$selector
           |isRunningSuite=$isRunningSuite
           |isEventForTest=$isEventForTest
           |event.fullyQualifiedName=${event.fullyQualifiedName}
           |event.selector=${event.selector}
           |event.status=${event.status}
           |event.throwable=$throwable""", // a problem on Scala 2.12: .stripMargin
        LogLevel.INFO
      )

      if !isRunningSuite then
        // running individual test case: ScalaCheck packages test methods into nested NestedTest tasks.
        require(Selectors.equal(selector, event.selector))
        event.status.name match
          case "Error" | "Failure" => failure(testId, throwable.get)
          case _ =>
      else
        // running suite
        // Events with overall results of suits are ignored; only events for individual test cases are processed:
        // - started/completed Gradle events are emitted in run();
        // - Gradle calculates overall result from the outcomes of the individual tests.
        // JUnit4 emits overall class failure events with a `TestSelector`.
        if
          isEventForTest &&
          !Selectors.equal(event.selector, TestSelector(className)) &&
          arrayFind(skipped, Selectors.equal(_, event.selector)).isEmpty
        then
          def reconstructStarted(): AnyRef =
            val eventTestId: AnyRef = idGenerator.generateId()
            started(
              parentId = testId,
              testId = eventTestId,
              // attribute nested test cases to the nested, not the nesting, suite
              className = Selectors.suiteId(event.selector).getOrElse(className),
              selector = event.selector,
              startTime = endTime - event.duration // TODO deal with negative durations?
            )
            eventTestId

          event.status.name match
            case "Success" =>
              completed(reconstructStarted(), endTime, ResultType.SUCCESS)

            case "Error" | "Failure" =>
              failure(reconstructStarted(), throwable.getOrElse(Exception("A FAKE EXCEPTION TO MAKE Gradle FEEL THE TEST FAILURE")))

            case _ =>
              skipped = arrayAppend(skipped, event.selector)
              if !isAllTests || throwable.nonEmpty || dryRun then
                completed(reconstructStarted(), endTime, ResultType.SKIPPED)

object RunTestClassProcessor:
  val rootTestSuiteIdPlaceholder: CompositeId = CompositeId(0L, 0L)
  
  def toString(task: Task): String = TaskDefs.toString(task.taskDef)
