package org.podval.tools.test.processor

import org.gradle.api.internal.tasks.testing.{DefaultTestClassDescriptor, DefaultTestMethodDescriptor,
  DefaultTestOutputEvent, TestClassProcessor, TestClassRunInfo, TestCompleteEvent, TestDescriptorInternal,
  TestResultProcessor, TestStartEvent}
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.testing.TestOutputEvent
import org.gradle.api.tasks.testing.TestResult.ResultType
import org.gradle.internal.id.IdGenerator
import org.gradle.internal.time.Clock
import org.podval.tools.test.RootTestSuiteIdPlaceholder
import org.podval.tools.test.exception.ExceptionConverter
import org.podval.tools.test.taskdef.{Selectors, TaskDefs, TestClassRun}
import org.podval.tools.util.Scala212Collections.{arrayAppend, arrayFind, arrayForAll, arrayForEach, arrayMap,
  arrayMkString, arrayPartition, stripPrefix}
import sbt.testing.{Event, NestedTestSelector, Runner, Selector, SuiteSelector, Task, TaskDef, TestSelector,
  TestWildcardSelector}
import scala.util.control.NonFatal

final class RunTestClassProcessor(
  includeTags: Array[String],
  excludeTags: Array[String],
  runningInIntelliJIdea: Boolean,
  logLevelEnabled: LogLevel,
  idGenerator: IdGenerator[?],
  clock: Clock
) extends TestClassProcessor:

  private val reportEvents: Boolean = false

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
    // JUnit4 - and thus MUnit which is based on it - set both the event's fullyQualifiedName and the selector
    // to something like <class name>.<method name>;
    // method names like this just look stupid,
    // but class names look like new classes to Gradle (since the event fingerprint says so),
    // which corrupts test reports.
    // I had to work around this.
    val testDescriptorInternal: TestDescriptorInternal = selector
      match
        case testSelector      : TestSelector       => Some(testSelector      .testName)
        case nestedTestSelector: NestedTestSelector => Some(nestedTestSelector.testName)
        case _ => None
      match
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
    testId: AnyRef,
    message: String,
    logLevel: LogLevel
  ): Unit =
    val isEnabled: Boolean = !runningInIntelliJIdea || (logLevel.ordinal >= logLevelEnabled.ordinal)
    given CanEqual[LogLevel, LogLevel] = CanEqual.derived
    if isEnabled then testResultProcessor.output(
      testId,
      DefaultTestOutputEvent(
        clock.getCurrentTime,
        if (logLevel == LogLevel.ERROR) || (logLevel == LogLevel.WARN)
        then TestOutputEvent.Destination.StdErr
        else TestOutputEvent.Destination.StdOut,
        s"$message\n"
      )
    )

  private def output(message: String): Unit = output(
    testId = RootTestSuiteIdPlaceholder.value,
    message = message,
    logLevel = LogLevel.LIFECYCLE,
  )

  private def testLogger(testId: AnyRef): sbt.testing.Logger = new sbt.testing.Logger:
    private def log(logLevel: LogLevel, message: String): Unit = output(testId, message, logLevel)
    override def ansiCodesSupported: Boolean = true
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
    val summary: String = runner.done()
    output(message = s"$frameworkName summary:\n$summary")
  )

  private var stoppedNow: Boolean = false
  override def stopNow(): Unit =
    stoppedNow = true
    stop()

  def processTestClass(testClassRunInfo: TestClassRunInfo): Unit = if !stoppedNow then
    val testClassRun: TestClassRun = testClassRunInfo.asInstanceOf[TestClassRun]
    // Note: Runner.tasks() can take more that one taskDef; I feed them in one by one.
    // Note: for each returned Task, task.taskDef is what was passed to Runner.tasks().
    val tasks: Array[Task] = getRunner(testClassRun).tasks(Array(testClassRun.taskDef))

    if reportEvents then reportTasks(testClassRun, tasks)

    arrayForEach(tasks, (task: Task) => run(
      parentId = null,
      task = task,
      isNested = false
    ))

  private def run(
    parentId: AnyRef,
    task: Task,
    isNested: Boolean
  ): Unit =
    val startTime: Long = clock.getCurrentTime
    val testId: AnyRef = idGenerator.generateId()
    val className: String = task.taskDef.fullyQualifiedName
    val selectors: Array[Selector] = task.taskDef.selectors
    if reportEvents then output(s"--- RunTestClassProcessor.run(${RunTestClassProcessor.toString(task)})")

    val isTests: Boolean = arrayForAll(selectors, RunTestClassProcessor.isTest)

    if !isNested then
      if !isTests then require(selectors.length == 1, "If not all non-nested selectors are tests, there can only be one")
    else
      require(selectors.length == 1, "Only one selector can be nested")
      val nestedSelector: Selector = selectors(0)
      require(RunTestClassProcessor.isTest(nestedSelector), s"Only individual tests can be nested, not $nestedSelector")

    // reconstruct the suite
    val selector: Selector = if !isNested && isTests then SuiteSelector() else selectors(0)

    started(
      parentId,
      testId,
      className,
      selector,
      startTime
    )

    try
      val nestedTasks: Array[Task] =
        try
          if reportEvents then output(s"--- Task(${RunTestClassProcessor.toString(task)}).execute()")
          val eventHandler: EventHandler = EventHandler(testId, className, selector, isTests)
          task.execute(
            eventHandler.handleEvent(_),
            Array(testLogger(testId))
          )
        catch case throwable@(_: NoClassDefFoundError | _: IllegalAccessError | NonFatal(_)) =>
          failure(testId, throwable)
          Array.empty

      arrayForEach(nestedTasks, (nestedTask: Task) =>
        if reportEvents then output(s"--- NESTED TASK FOR $className $selector: ${RunTestClassProcessor.toString(nestedTask)}")
        require(!RunTestClassProcessor.isTest(selector), s"Individual tests like $selector can not have nested tests")
        run(
          parentId = testId,
          task = nestedTask,
          isNested = true
        )
      )
    finally completed(
      testId = testId,
      endTime = clock.getCurrentTime,
      result = null
    )

  // Although it is tempting to help the test frameworks out by filtering tests based on their tags
  // returned by the test framework in `task.tags`, it is:
  // - unnecessary, since all the test frameworks plugin supports that support tagging accept
  //   arguments that allow them to do the filtering internally;
  // - destructive, since none of the test frameworks plugin supports populate `task.tags`,
  //   so with explicit tag inclusions, none of the tests run!

  final private class EventHandler(
    testId: AnyRef,
    className: String,
    selector: Selector,
    isTests: Boolean
  ):
    // JUnit4 emits SUCCESS event for tests that were skipped because of a falsified assumption;
    // we suppress such events lest Gradle report two copies of a test - one skipped, one passed.
    private var skipped: Array[Selector] = Array.empty
    
    def handleEvent(event: Event): Unit =
      val endTime: Long = clock.getCurrentTime

      val throwable: Option[Throwable] = if event.throwable.isEmpty then None else Some(event.throwable.get)
      
      if reportEvents then output(
        s"""--- GOT EVENT
           |className=$className
           |selector=$selector
           |event.fullyQualifiedName=${event.fullyQualifiedName}
           |event.selector=${event.selector}
           |event.status=${event.status}
           |event.throwable=$throwable
           |""".stripMargin
      )

      // Note: we ignore events with overall results of suits:
      // - started/completed Gradle events are emitted in run();
      // - Gradle calculates overall result from the outcomes of the individual tests.
      // JUnit4 emits overall class failure events with a `TestSelector`.
      val isTest: Boolean = RunTestClassProcessor.isTest(selector)
      val forClass: Boolean = !isTest && (
        Selectors.equal(selector, event.selector) ||
        Selectors.equal(event.selector, TestSelector(className))
      )
      if !forClass && arrayFind(skipped, Selectors.equal(_, event.selector)).isEmpty then
        val isNested: Boolean = !isTest
        lazy val eventTestId: AnyRef = if !isNested then testId else idGenerator.generateId()

        def reconstructStarted(): Unit = started(
          parentId = testId,
          testId = eventTestId,
          className = className,
          selector = event.selector,
          startTime = endTime - event.duration // TODO deal with negative durations?
        )

        event.status.name match
          case "Success" =>
            if isNested then
              reconstructStarted()
              completed(eventTestId, endTime, ResultType.SUCCESS)

          // Note: failure is reported for the overall test, not just for nested methods:
          // ScalaCheck packages test methods into nested tasks.
          // Note: It is possible, albeit not nice, for the test framework to not populate the `event.throwable`
          // of the `Failure` event; Scala.js's JUnit used to do this (see https://github.com/scala-js/scala-js/pull/5132).
          // Gradle, on the other hand, treats a test as failed only when it receives a `throwable` for the test -
          // otherwise, although XML report does record the failure, HTML report does not, nor does Gradle build fail.
          // This is why I supply a synthesized event for *method* failures if one did not come up from the framework.
          case "Error" | "Failure" =>
            if isNested then
              reconstructStarted()
            throwable
              .orElse(if !isNested then None else Some(Exception("A FAKE EXCEPTION TO MAKE Gradle FEEL THE TEST FAILURE")))
              .foreach(failure(eventTestId, _))

          // Note: When I am running some test methods explicitly included by a filter,
          // I do not want to see other methods mentioned in the test report,
          // just as I do not want to see other skipped tests class there.
          // But I do want to see tests explicitly ignored in code (e.g., in ScalaTest, or JUnit4's falsified assumptions).
          // Note: If I call reconstructStarted(), Gradle figures out that the test was skipped from
          // the suite completes, so I do not need to explicitly send complete(Skipped) - but why not?
          case "Skipped" | "Ignored" | "Canceled" | "Pending" | _ =>
            skipped = arrayAppend(skipped, event.selector)
            if isNested && (!isTests || throwable.nonEmpty) then
              reconstructStarted()
              completed(eventTestId, endTime, ResultType.SKIPPED)

  private def reportTasks(
    testClassRun: TestClassRun,
    tasks: Array[Task]
  ): Unit =
    val taskDef: TaskDef = testClassRun.taskDef
    val (same: Array[Task], different: Array[Task]) = 
      arrayPartition(tasks, (task: Task) => TaskDefs.equal(taskDef, task.taskDef))

    def out(message: String): Unit =
      output(s"--- ${testClassRun.frameworkName} $message")

    val taskDefString: String = TaskDefs.toString(taskDef)
    if same.length > 1 then out(s"MULTIPLE TASKS FOR THE $taskDefString") else if different.isEmpty then
      if same.isEmpty
      then out(s"REJECTED $taskDefString")
      else out(s"ACCEPTED $taskDefString")
    else
      val differentStr: String = arrayMkString(arrayMap(different, RunTestClassProcessor.toString), "", "\n", "")
      if same.isEmpty
      then out(s"REPLACED $taskDefString with: $differentStr")
      else out(s"ADDED TO $taskDefString: $differentStr")

object RunTestClassProcessor:
  def toString(task: Task): String = TaskDefs.toString(task.taskDef)

  def isTest(selector: Selector): Boolean = selector match
    case _: TestSelector | _: NestedTestSelector | _: TestWildcardSelector => true
    case _ => false
