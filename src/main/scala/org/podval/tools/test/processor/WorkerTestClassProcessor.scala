package org.podval.tools.test.processor

import org.gradle.api.internal.tasks.testing.{DefaultTestClassDescriptor, DefaultTestMethodDescriptor,
  DefaultTestOutputEvent, TestClassProcessor, TestClassRunInfo, TestCompleteEvent, TestDescriptorInternal,
  TestResultProcessor, TestStartEvent}
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.testing.TestOutputEvent
import org.gradle.api.tasks.testing.TestResult.ResultType
import org.gradle.internal.id.IdGenerator
import org.gradle.internal.time.Clock
import org.podval.tools.test.RootTestSuiteItPlaceholder
import org.podval.tools.test.exception.ExceptionConverter
import org.podval.tools.test.taskdef.{Selectors, TaskDefTestSpec, TaskDefs, Tasks}
import org.podval.tools.util.Scala212Collections.{arrayAppend, arrayFind, arrayForEach, arrayMap, arrayMkString,
  arrayPartition, stripPrefix}
import sbt.testing.{Event, NestedTestSelector, Runner, Selector, Status, Task, TaskDef, TestSelector}
import scala.util.control.NonFatal

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
    // to something like <class name>.<method name>;
    // method names like this just look stupid,
    // but class names look like new classes to Gradle (since the event fingerprint says so),
    // which corrupts test reports.
    // I had to work around this.
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
    testId = RootTestSuiteItPlaceholder.value,
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

    // Note: Runner.tasks() can take more that one taskDef; I feed them in one by one.
    // Note: for each returned Task, task.taskDef is what was passed to Runner.tasks().
    val tasks: Array[Task] = getRunner(taskDefTestSpec.frameworkOrName).tasks(Array(taskDefTestSpec.taskDef))

    if reportEvents then reportTasks(taskDefTestSpec, tasks)

    // Note: when running individual test method(s), selector of the task is for a test, not a suite.
    arrayForEach(tasks, (task: Task) => run(
      parentId = null,
      task = task
    ))

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
    val selector: Selector = Tasks.getSelector(task)
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
          if reportEvents then output(s"--- Task(${Tasks.toString(task)}).execute()")
          val eventHandler: EventHandler = EventHandler(testId, className, selector)
          task.execute(
            eventHandler.handleEvent(_),
            Array(testLogger(testId))
          )
        catch case throwable@(_: NoClassDefFoundError | _: IllegalAccessError | NonFatal(_)) =>
          failure(testId, throwable)
          Array.empty

      arrayForEach(nestedTasks, (nestedTask: Task) =>
        if reportEvents then output(s"--- NESTED TASK FOR $className $selector: ${Tasks.toString(nestedTask)}")
        val nestedSelector: Selector = Tasks.getSelector(nestedTask)
        require(!Selectors.isTest(selector), s"Individual tests like $selector can not have nested tests like $nestedSelector")
        require(Selectors.isTest(nestedSelector), s"Only individual tests can be nested, not $nestedSelector")
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

      val eventSelector: Selector = event.selector
      val eventResult: ResultType = WorkerTestClassProcessor.toResultType(event.status)
      val throwable: Option[Throwable] = if event.throwable.isEmpty then None else Some(event.throwable.get)
      
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

      val isSameSelector: Boolean = Selectors.equal(selector, eventSelector)
      val isTest: Boolean = Selectors.isTest(selector)

      // Note: when running individual test method(s), the following is false:
      //   require(isSameSelector || !isTest && Selectors.isTest(eventSelector))
      
      // Note: when running individual test method(s),
      // test framework may (and JUnit4 does) emit events for skipped methods;
      // such events are not currently propagated by the code - which is, I think, correct:
      // if I am running one test method, I do not want to see other method mentioned in the test report,
      // just as I do not want to see other skipped tests class there.
      
      // Note: we ignore events with overall results of suits:
      // - started/completed Gradle events are emitted in run();
      // - Gradle calculates overall result from the outcomes of the individual tests.
      // JUnit4 emits overall class failure events with a `TestSelector`.
      
      val forClass: Boolean = !isTest && (isSameSelector || Selectors.equal(eventSelector, TestSelector(className)))
      if !forClass && arrayFind(skipped, Selectors.equal(_, eventSelector)).isEmpty then
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
      arrayPartition(tasks, (task: Task) => TaskDefs.taskDefsEqual(taskDef, task.taskDef))

    def out(message: String): Unit =
      val frameworkName: String = TaskDefTestSpec.frameworkName(taskDefTestSpec.frameworkOrName)
      output(s"--- $frameworkName $message")

    val taskDefString: String = TaskDefs.toString(taskDef)
    if same.length > 1 then out(s"MULTIPLE TASKS FOR THE $taskDefString") else if different.isEmpty then
      if same.isEmpty
      then out(s"REJECTED $taskDefString")
      else out(s"ACCEPTED $taskDefString")
    else
      val differentStr: String = arrayMkString(arrayMap(different, Tasks.toString), "", "\n", "")
      if same.isEmpty
      then out(s"REPLACED $taskDefString with: $differentStr")
      else out(s"ADDED TO $taskDefString: $differentStr")

object WorkerTestClassProcessor:
  def toResultType(status: Status): ResultType =
    // When `scalajs-test-interface` is used instead of the `test-interface`, I get:
    //   Class sbt.testing.Status does not have member field 'sbt.testing.Status Success'
    //    given CanEqual[Status, Status] = CanEqual.derived
    //    status match
    //    case Status.Success  => ResultType.SUCCESS
    //    case Status.Error    => ResultType.FAILURE
    //    case Status.Failure  => ResultType.FAILURE
    //    case Status.Skipped  => ResultType.SKIPPED
    //    case Status.Ignored  => ResultType.SKIPPED
    //    case Status.Canceled => ResultType.SKIPPED
    //    case Status.Pending  => ResultType.SKIPPED
    // This approach works for both:
    val name: String = status.name()
    if name == "Success" then ResultType.SUCCESS else
    if name == "Error"   then ResultType.FAILURE else
    if name == "Failure" then ResultType.FAILURE else
      ResultType.SKIPPED
