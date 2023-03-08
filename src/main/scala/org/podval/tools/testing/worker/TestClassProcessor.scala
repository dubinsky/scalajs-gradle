package org.podval.tools.testing.worker

import org.gradle.api.internal.tasks.testing.{DefaultTestMethodDescriptor, DefaultTestOutputEvent, TestClassRunInfo,
  TestCompleteEvent, TestDescriptorInternal, TestResultProcessor, TestStartEvent, WorkerTestClassProcessorFactory}
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.testing.TestResult.ResultType
import org.gradle.api.tasks.testing.{TestFailure, TestOutputEvent}
import org.gradle.internal.id.CompositeIdGenerator.CompositeId
import org.gradle.internal.id.IdGenerator
import org.gradle.internal.service.ServiceRegistry
import org.gradle.internal.time.Clock
import org.podval.tools.testing.exceptions.ExceptionConverter
import org.podval.tools.testing.framework.FrameworkDescriptor
import org.podval.tools.testing.serializer.{TaskDefTestSpec, TaskDefTestSpecWriter}
import sbt.testing.{Event, Framework, NestedTestSelector, OptionalThrowable, Runner, Selector, Status, Task, TaskDef,
  TestSelector}
import java.io.File
import java.net.URLClassLoader
import scala.util.control.NonFatal

final class TestClassProcessor(
  testTagsFilter: TestTagsFilter,
  runningInIntelliJIdea: Boolean,
  logLevelEnabled: LogLevel,
  idGenerator: IdGenerator[AnyRef],
  clock: Clock,
) extends org.gradle.api.internal.tasks.testing.TestClassProcessor:

  private var testResultProcessorOpt: Option[TestResultProcessor] = None
  private def testResultProcessor: TestResultProcessor = testResultProcessorOpt.get

  override def startProcessing(testResultProcessor: TestResultProcessor): Unit =
    testResultProcessorOpt = Some(testResultProcessor)

  private var runners: Map[String, Runner] = Map.empty

  private def getRunner(testFramework: Either[String, Framework]): Runner = synchronized {
    val frameworkName: String = testFramework.fold(identity, _.name)
    runners.getOrElse(frameworkName, {
      val runner: Runner = TestClassProcessor.runner(
        getFramework(testFramework),
        testTagsFilter
      )
      runners = runners.updated(frameworkName, runner)
      runner
    })
  }

  private def getFramework(testFramework: Either[String, Framework]): Framework = testFramework match
    case Right(framework) =>
      // not forking: just use the framework and its classloader
      framework
    case Left(frameworkName) =>
      // forking: instantiate
      FrameworkDescriptor(frameworkName)
        .newInstance
        .asInstanceOf[Framework]

  /**
   * Completes any pending or asynchronous processing. Blocks until all processing is complete.
   */
  override def stop(): Unit =
    for (frameworkName: String, runner: Runner) <- runners.toSeq do
      // TODO [output] according to the Runner documentation, summary returned was already sent to the logger?
      val summary: String = runner.done()
      output(
        testId = TaskDefEx.rootTestSuiteIdPlaceholder,
        message = s"$frameworkName summary:\n$summary",
        logLevel = LogLevel.LIFECYCLE
      )

  /**
   * Stops any pending or asynchronous processing immediately.
   * Any test class assigned to this processor, but not yet run will not have results in the output.
   */
  private var stoppedNow: Boolean = false
  override def stopNow(): Unit =
    stoppedNow = true
    stop()

  override def processTestClass(testClassRunInfo: TestClassRunInfo): Unit =
    if !stoppedNow then
      val taskDefTestSpec: TaskDefTestSpec = TaskDefTestSpecWriter.read(testClassRunInfo)
      val taskDef: TaskDef = taskDefTestSpec.taskDef

      // Note: tasks() can take more that one taskDef, but I feed them in one by one:
      val tasks: Array[Task] = getRunner(taskDefTestSpec.framework).tasks(Array(taskDef))

      val (same: Array[Task], different: Array[Task]) =
        tasks.partition((task: Task) => TaskDefEx.taskDefsEqual(taskDef, task.taskDef))

      def out(message: String): Unit =
        ()
//        val frameworkName: String = taskDefTestSpec.framework.fold(identity, _.name)
//        output(
//          testId = TaskDefEx.rootTestSuiteIdPlaceholder, // TODO does not work: before root?!
//          logLevel = LogLevel.LIFECYCLE,
//          message = s"--- $frameworkName $message"
//        )

      val taskDefString = TaskDefEx.toString(taskDef)
      if same.length > 1 then out(s"--- MULTIPLE TASKS FOR THE $taskDefString") else
        if different.isEmpty then
          if same.isEmpty
          then out(s"REJECTED $taskDefString")
          else out(s"ACCEPTED $taskDefString")
        else
          val differentStr: String = different.map(_.taskDef).map(TaskDefEx.toString).mkString("\n")
          if same.isEmpty
          then out(s"REPLACED $taskDefString with: $differentStr")
          else out(s"ADDED TO $taskDefString: $differentStr")

      // For test classes that do not have tests, empty tasks array is returned;
      // there is no need nor point to report such an occurrence:
      // nothing shows up un the Idea's test tree.
      // I never saw ScalaTest (the only framework I use) return more than one task,
      // so I do not yet know how would that be reported by Gradle/Idea.
      for task: Task <- tasks do run(null, task)

  private def run(
    parentId: AnyRef,
    task: Task
  ): Unit =
    require(task.taskDef.selectors.length == 1, s"--- MORE THAN ONE SELECTOR FOR THE TASK!")

    val testId: AnyRef = idGenerator.generateId()

    var isTestCompleted: Boolean = false

    val startTime: Long = clock.getCurrentTime
    testResultProcessor.started(TaskDefEx.toTestDescriptorInternal(testId, task.taskDef), TestStartEvent(startTime, parentId))

    val isAllowed: Boolean =
      val tags: Array[String] = task.tags
      def isListed(list: Array[String]): Boolean = tags.exists(list.contains)
      (testTagsFilter.include.isEmpty || isListed(testTagsFilter.include)) && !isListed(testTagsFilter.exclude)

    if !isAllowed then
      // skipped test
      testResultProcessor.completed(testId, TestCompleteEvent(startTime, ResultType.SKIPPED))
    else
      try
        val nestedTasks: Seq[Task] =
          try
            task.execute(
              (event: Event) =>
                if isTestCompleted then
                  throw IllegalStateException(s"Received event for a completed test ${TaskDefEx.toString(task.taskDef)}")
                isTestCompleted = !handleEvent(testId, task.taskDef, event),
              Array(testLogger(testId))
            ).toSeq
          catch case throwable@(_: NoClassDefFoundError | _: IllegalAccessError | NonFatal(_)) =>
            testResultProcessor.failure(testId, TestFailure.fromTestFrameworkFailure(throwable))
            Seq.empty

        // Note: ScalaCheck does individual test methods as nested tasks
        for nestedTask: Task <- nestedTasks do
//          output(
//            testId = TaskDefEx.rootTestSuiteIdPlaceholder,
//            logLevel = LogLevel.LIFECYCLE,
//            message = s"----- GOT NESTED TASK FOR ${TaskDefEx.toString(task.taskDef)}: ${TaskDefEx.toString(nestedTask.taskDef)}"
//          )
          TaskDefEx.verifyCanHaveNestedTest(task.taskDef, nestedTask.taskDef)
          run(testId, nestedTask)
      finally
        if !isTestCompleted then
          testResultProcessor.completed(testId, TestCompleteEvent(clock.getCurrentTime))

  private def handleEvent(
    testId: AnyRef,
    taskDef: TaskDef,
    event: Event
  ): Boolean =
    val endTime: Long = clock.getCurrentTime

    // Note: Assuming that if the event is for the taskDef, selector is the same,
    // and if not - it is a *test method* in the same class;
    // MUnit and JUnit4 set both the event's fullyQualifiedName and the selector
    // to something like <class name>.<method name>, which confuses Gradle into thinking
    // that that is a new class name, since the event fingerprint says so...
    val isNestedTest: Boolean = !taskDef.selectors.head.equals(event.selector) // not !TaskDefEx.taskDefsEqual(taskDef, nestedTaskDef)
    val eventTest: TestDescriptorInternal =
      if !isNestedTest then TaskDefEx.toTestDescriptorInternal(testId, taskDef) else
        def cleanUpName(name: String): String =
          if name.startsWith(taskDef.fullyQualifiedName+".") then name.substring(taskDef.fullyQualifiedName.length+1) else name

        val selector: Selector = event.selector match
          case testSelector      : TestSelector       => TestSelector      (                            cleanUpName(testSelector      .testName))
          case nestedTestSelector: NestedTestSelector => NestedTestSelector(nestedTestSelector.suiteId, cleanUpName(nestedTestSelector.testName))
          // nothing else is possible ;)

        val nestedTaskDef: TaskDef = TaskDef(
          taskDef.fullyQualifiedName, // not event.fullyQualifiedName
          taskDef.fingerprint,        // not event.fingerprint
          false,
          Array(selector)
        )

//        output(
//          testId = TaskDefEx.rootTestSuiteIdPlaceholder,
//          logLevel = LogLevel.LIFECYCLE,
//          message = s"----- GOT NESTED EVENT FOR ${TaskDefEx.toString(taskDef)}: ${TaskDefEx.toString(nestedTaskDef)}"
//        )
        TaskDefEx.verifyCanHaveNestedTest(taskDef, nestedTaskDef)
        val nestedTestId: AnyRef = idGenerator.generateId()
        val nestedTest: TestDescriptorInternal = TaskDefEx.toTestDescriptorInternal(nestedTestId, nestedTaskDef)
        // Note: for implied eventTest we reconstruct the 'started' event
        testResultProcessor.started(nestedTest, TestStartEvent(endTime - event.duration, testId))
        nestedTest

    TestClassProcessor.toOption(event.throwable).foreach((throwable: Throwable) =>
      testResultProcessor.failure(eventTest.getId, ExceptionConverter.convert(throwable))
    )

    testResultProcessor.completed(eventTest.getId, TestCompleteEvent(endTime, TestClassProcessor.toResultType(event.status)))

    isNestedTest

  private def testLogger(testId: AnyRef): sbt.testing.Logger = new sbt.testing.Logger:
    private def log(logLevel: LogLevel, message: String): Unit = output(
      testId = testId,
      message = message,
      logLevel = logLevel
    )

    // TODO [output] I do not see any issues with the colors on in Idea when the output is delivered through
    // the proper channels, so there seems to be no need for "!runningInIntelliJIdea" here
    // when running ScalaTest - but MUnit's and UTest's output gets garbled with the color escape sequences
    // even *with* the flag...
    override def ansiCodesSupported: Boolean = true
    override def error(message: String): Unit = log(LogLevel.ERROR, message)
    override def warn(message: String): Unit = log(LogLevel.WARN, message)
    override def info(message: String): Unit = log(LogLevel.INFO, message)
    override def debug(message: String): Unit = log(LogLevel.DEBUG, message)
    override def trace(throwable: Throwable): Unit =
      testResultProcessor.failure(testId, TestFailure.fromTestFrameworkFailure(throwable))

  private given CanEqual[LogLevel, LogLevel] = CanEqual.derived
  private def output(
    testId: AnyRef,
    message: String,
    logLevel: LogLevel
  ): Unit =
    val isEnabled: Boolean = !runningInIntelliJIdea || (logLevel.ordinal >= logLevelEnabled.ordinal)
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
  // Note: this one is Java-serialized, so I am using serializable types for parameters
  final class Factory(
    testTagsFilter: TestTagsFilter,
    runningInIntelliJIdea: Boolean,
    logLevelEnabled: LogLevel
  ) extends WorkerTestClassProcessorFactory with Serializable:

    override def create(serviceRegistry: ServiceRegistry): TestClassProcessor = TestClassProcessor(
      testTagsFilter = testTagsFilter,
      runningInIntelliJIdea = runningInIntelliJIdea,
      logLevelEnabled = logLevelEnabled,
      idGenerator = serviceRegistry.get(classOf[IdGenerator[AnyRef]]),
      clock = serviceRegistry.get(classOf[Clock])
    )

  def runner(framework: Framework, testTagsFilter: TestTagsFilter): Runner =
    val frameworkName: String = framework.name
    val args: Array[String] = FrameworkDescriptor(frameworkName).args(testTagsFilter)
    // Note: we are running the runner in *this* JVM, so remote arguments are not used?
    val remoteArgs: Array[String] = Array.empty
    val frameworkClassLoader: ClassLoader = framework.getClass.getClassLoader
    framework.runner(args, remoteArgs, frameworkClassLoader)

  def toOption(optionalThrowable: OptionalThrowable): Option[Throwable] =
    if optionalThrowable.isEmpty then None else Some(optionalThrowable.get)

  private given CanEqual[Status, Status] = CanEqual.derived

  def toResultType(status: Status): ResultType = status match
    case Status.Success  => ResultType.SUCCESS
    case Status.Error    => ResultType.FAILURE
    case Status.Failure  => ResultType.FAILURE
    case Status.Skipped  => ResultType.SKIPPED
    case Status.Ignored  => ResultType.SKIPPED
    case Status.Canceled => ResultType.SKIPPED
    case Status.Pending  => ResultType.SKIPPED
