package org.podval.tools.test.processor

import org.gradle.api.internal.tasks.testing.{DefaultTestClassDescriptor, DefaultTestMethodDescriptor,
  DefaultTestOutputEvent, TestClassProcessor, TestClassRunInfo, TestCompleteEvent, TestDescriptorInternal,
  TestResultProcessor, TestStartEvent}
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.testing.TestOutputEvent
import org.gradle.api.tasks.testing.TestResult.ResultType
import org.gradle.internal.id.IdGenerator
import org.gradle.internal.time.Clock
import org.podval.tools.test.{TaskDefTestSpec, TestInterface}
import org.podval.tools.test.exception.ExceptionConverter
import org.podval.tools.util.Scala212Collections.{arrayAppend, arrayFind, arrayForEach, arrayMap, arrayMkString, 
  arrayPartition, stripPrefix}
import sbt.testing.{Event, NestedTestSelector, Runner, Selector, Task, TaskDef, TestSelector}
import scala.util.control.NonFatal

// TODO Scala 2.12: there is still a reference to scala/Predef$.refArrayOps somewhere...
final class WorkerTestClassProcessor(
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
    // to something like <class name>.<method name>, which confuses Gradle into thinking
    // that that is a new class name, since the event fingerprint says so...
    val testDescriptorInternal: TestDescriptorInternal = selector
      match
        case testSelector: TestSelector => Some(testSelector.testName)
        case nestedTestSelector: NestedTestSelector => Some(nestedTestSelector.testName)
        case _ => None
      match
        case None => DefaultTestClassDescriptor(testId, className)
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
    testId = TestInterface.rootTestSuiteIdPlaceholder,
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

  // I'd go with var runners: Map[String, Runner]; runners.getOrElse; runners.updated;
  // but since `map.updated` does not work with Scala 2.12...
  private var runners: Array[(String, Runner)] = Array.empty
  private def getRunner(frameworkOrName: TaskDefTestSpec.FrameworkOrName): Runner = synchronized:
    val frameworkName: String = TaskDefTestSpec.frameworkName(frameworkOrName)
    arrayFind(runners, _._1 == frameworkName).map(_._2).getOrElse:
      val runner: Runner = TaskDefTestSpec.makeRunner(
        frameworkOrName,
        includeTags,
        excludeTags
      )
      runners = arrayAppend(runners, (frameworkName, runner))
      runner

  /**
   * Completes any pending or asynchronous processing. Blocks until all processing is complete.
   */
  override def stop(): Unit = arrayForEach(runners, (frameworkName: String, runner: Runner) =>
    val summary: String = runner.done()
    output(message = s"$frameworkName summary:\n$summary")
  )

  /**
   * Stops any pending or asynchronous processing immediately.
   * Any test class assigned to this processor, but not yet run will not have results in the output.
   */
  private var stoppedNow: Boolean = false
  override def stopNow(): Unit =
    stoppedNow = true
    stop()

  override def processTestClass(testClassRunInfo: TestClassRunInfo): Unit = if !stoppedNow then
    val taskDefTestSpec: TaskDefTestSpec = TaskDefTestSpec.get(testClassRunInfo)

    // Note: tasks() can take more that one taskDef; I feed them in one by one.
    // Note: task.taskDef: Returns the TaskDef that was used to request this Task.
    val tasks: Array[Task] = getRunner(taskDefTestSpec.frameworkOrName).tasks(Array(taskDefTestSpec.taskDef))

    if reportEvents then reportTasks(taskDefTestSpec, tasks)

    arrayForEach(tasks, (task: Task) =>
      require(!TestInterface.isTest(TestInterface.getSelector(task)))
      run(
        parentId = null,
        task = task
      )
    )

  // Note: although this looks recursive,
  // there are only two levels possible with the current rules on test nesting:
  // - overall suite (test class)
  // - individual test from that suite.
  private def run(
    parentId: AnyRef,
    task: Task
  ): Unit =
    val startTime: Long = clock.getCurrentTime
    val testId: AnyRef = idGenerator.generateId()
    val selector: Selector = TestInterface.getSelector(task)
    val className: String = task.taskDef.fullyQualifiedName
    if reportEvents then output(s"--- TestClassProcessor.run($className, $selector)")

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
          if reportEvents then output(s"--- Task(${TestInterface.toString(task)}).execute()")
          val eventHandler: EventHandler = EventHandler(testId, className, selector)
          task.execute(
            eventHandler.handleEvent(_),
            Array(testLogger(testId))
          )
        catch case throwable@(_: NoClassDefFoundError | _: IllegalAccessError | NonFatal(_)) =>
          failure(testId, throwable)
          Array.empty

      arrayForEach(nestedTasks, (nestedTask: Task) =>
        if reportEvents then output(s"--- NESTED TASK FOR $className $selector: ${TestInterface.toString(nestedTask)}")
        val nestedSelector: Selector = TestInterface.getSelector(nestedTask)
        require(!TestInterface.isTest(selector), s"Individual tests like $selector can not have nested tests like $nestedSelector")
        require(TestInterface.isTest(nestedSelector), s"Only individual tests can be nested, not $nestedSelector")
        run(
          parentId = testId,
          task = nestedTask
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
    selector: Selector
  ):
    // JUnit4 emits SUCCESS event for tests that were skipped because of a falsified assumption;
    // we suppress such events lest Gradle report two copies of a test - one skipped, one passed.
    private var skipped: Array[Selector] = Array.empty
    
    def handleEvent(event: Event): Unit =
      val endTime: Long = clock.getCurrentTime
      val eventResult: ResultType = TestInterface.toResultType(event.status)
      val throwable: Option[Throwable] = if event.throwable.isEmpty then None else Some(event.throwable.get)
      val eventSelector: Selector = event.selector

      val isSameSelector: Boolean = TestInterface.selectorsEqual(selector, eventSelector)
      val isTest: Boolean = TestInterface.isTest(selector)
      val isEventTest: Boolean = TestInterface.isTest(eventSelector)

      // TODO verify event.fullyQualifiedName also?
      require(isSameSelector || !isTest && isEventTest)

      if reportEvents then output(
        s"""--- GOT EVENT
           |className=$className
           |selector=$selector
           |event.fullyQualifiedName=${event.fullyQualifiedName}
           |eventSelector=$eventSelector
           |eventResult=$eventResult
           |throwable=$throwable
           |""".stripMargin
      )

      // We ignore events with overall results of suits:
      // - started/completed Gradle events are emitted in run();
      // - Gradle calculates overall result from the outcomes of the individual tests.
      // JUnit4 emits overall class failure events with a `TestSelector`.
      val forClass: Boolean = !isTest && (isSameSelector || TestInterface.selectorsEqual(eventSelector, TestSelector(className)))
      if !forClass && arrayFind(skipped, TestInterface.selectorsEqual(_, eventSelector)).isEmpty then
        val isNested: Boolean = !isTest
        val eventTestId: AnyRef = if !isNested then testId else idGenerator.generateId()

        if isNested then started(
          parentId = testId,
          testId = eventTestId,
          className = className,
          selector = eventSelector,
          startTime = endTime - event.duration
        )

        given CanEqual[ResultType, ResultType] = CanEqual.derived
        eventResult match
          // Note: failure is reported for the overall test, not just for nested methods:
          // ScalaCheck packages test methods into nested tasks.
          // Note: It is possible, albeit not nice, for the test framework to not populate the `event.throwable`
          // of the `Failure` event; Scala.js's JUnit used to do this (see https://github.com/scala-js/scala-js/pull/5132).
          // Gradle, on the other hand, treats a test as failed only when it receives a `throwable` for the test -
          // otherwise, although XML report does record the failure, HTML report does not, nor does Gradle build fail.
          // This is why I supply a synthesized event for *method* failures if one did not come up from the framework.
          case ResultType.FAILURE => throwable
            .orElse(if !isNested then None else Some(Exception("A FAKE EXCEPTION TO MAKE Gradle FEEL THE TEST FAILURE")))
            .foreach(failure(eventTestId, _))
          case ResultType.SUCCESS => if isNested then completed(eventTestId, endTime, eventResult)
          case ResultType.SKIPPED => skipped = arrayAppend(skipped, eventSelector)

  private def reportTasks(
    taskDefTestSpec: TaskDefTestSpec,
    tasks: Array[Task]
  ): Unit =
    val taskDef: TaskDef = taskDefTestSpec.taskDef
    val (same: Array[Task], different: Array[Task]) = 
      arrayPartition(tasks, (task: Task) => TestInterface.taskDefsEqual(taskDef, task.taskDef))

    def out(message: String): Unit =
      val frameworkName: String = TaskDefTestSpec.frameworkName(taskDefTestSpec.frameworkOrName)
      output(s"--- $frameworkName $message")

    val taskDefString: String = TestInterface.toString(taskDef)
    if same.length > 1 then out(s"MULTIPLE TASKS FOR THE $taskDefString") else if different.isEmpty then
      if same.isEmpty
      then out(s"REJECTED $taskDefString")
      else out(s"ACCEPTED $taskDefString")
    else
      val differentStr: String = arrayMkString(arrayMap(different, TestInterface.toString), "", "\n", "")
      if same.isEmpty
      then out(s"REPLACED $taskDefString with: $differentStr")
      else out(s"ADDED TO $taskDefString: $differentStr")
