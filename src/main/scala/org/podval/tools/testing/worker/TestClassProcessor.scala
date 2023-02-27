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
import org.podval.tools.testing.serializer.TaskDefTestWriter
import sbt.testing.{AnnotatedFingerprint, Event, Fingerprint, Framework, OptionalThrowable, Runner, Selector, Status,
  SubclassFingerprint, Task, TaskDef, TestWildcardSelector}
import java.net.{URL, URLClassLoader}
import scala.util.control.NonFatal

final class TestClassProcessor(
  testClassPath: Array[URL],
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

  // TODO [classpath] I have no idea why this works only if testClassLoader
  // contains *just* the tests and nothing more - nor how did I first figure it out :))
  private val testClassLoader: ClassLoader = URLClassLoader(testClassPath)
  private var runners: Map[String, Runner] = Map.empty
  private def getRunner(framework: Either[String, Framework]): Runner = synchronized {
    val frameworkName: String = framework.fold(identity, _.name)
    runners.getOrElse(frameworkName, {
      val args: Array[String] = FrameworkDescriptor(frameworkName).args(testTagsFilter)
      // Note: we are running the runner in *this* JVM, so remote arguments are not used?
      val runnerFramework: Framework = framework.fold(FrameworkDescriptor(_).instantiate, identity)
      val runner: Runner = runnerFramework.runner(args, Array.empty, testClassLoader)
      runners = runners.updated(frameworkName, runner)
      runner
    })
  }

  /**
   * Completes any pending or asynchronous processing. Blocks until all processing is complete.
   */
  override def stop(): Unit =
    for (frameworkName: String, runner: Runner) <- runners.toSeq do
      // TODO [output] according to the Runner documentation, summary returned was already sent to the logger?
      val summary: String = runner.done()
      output(
        testId = TestClassProcessor.rootTestSuiteIdPlaceholder,
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
      val testWithoutId: TaskDefTest = testClassRunInfo match
        case test: TaskDefTest => test
        case _ => TaskDefTestWriter.read(testClassRunInfo.getTestClassName)
      val test: TaskDefTest = testWithoutId.withId(idGenerator.generateId())

      // Note: tasks() can take more that one taskDef, but I feed them in one by one:
      val tasks: Array[Task] = getRunner(test.framework).tasks(Array(test.taskDef))
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

    val startTime: Long = clock.getCurrentTime
    testResultProcessor.started(test.toTestDescriptorInternal, TestStartEvent(startTime, parentId))

    val isAllowed: Boolean =
      val tags: Array[String] = task.tags
      def isListed(list: Array[String]): Boolean = tags.exists(list.contains)
      (testTagsFilter.include.isEmpty || isListed(testTagsFilter.include)) && !isListed(testTagsFilter.exclude)

    if !isAllowed then
      // skipped test
      testResultProcessor.completed(test.id, TestCompleteEvent(startTime, ResultType.SKIPPED))
    else
      try
        val nestedTasks: Seq[Task] =
          try
            task.execute(
              (event: Event) =>
                if isTestCompleted then throw IllegalStateException(s"Received event for a completed test $test")
                isTestCompleted = !handleEvent(test, event),
              Array(testLogger(test))
            ).toSeq
          catch case throwable@(_: NoClassDefFoundError | _: IllegalAccessError | NonFatal(_)) =>
            testResultProcessor.failure(test.id, TestFailure.fromTestFrameworkFailure(throwable))
            Seq.empty

        for task: Task <- nestedTasks do
          val taskDef: TaskDef = task.taskDef
          TestClassProcessor.verifyCanHaveNestedTest(test, taskDef)
          val nestedTest: TaskDefTest = TaskDefTest(
            id = idGenerator.generateId(),
            framework = test.framework,
            taskDef = taskDef
          )
          run(test.id, nestedTest, task)
      finally
        if !isTestCompleted then
          testResultProcessor.completed(test.id, TestCompleteEvent(clock.getCurrentTime, ResultType.SUCCESS))

  private def handleEvent(
    test: TaskDefTest,
    event: Event
  ): Boolean =
    val endTime: Long = clock.getCurrentTime
    val selector: Selector = event.selector
    val className: String = event.fullyQualifiedName
    val taskDef: TaskDef = TaskDef(className, event.fingerprint, false, Array(selector))
    val isNestedTest: Boolean = !TestClassProcessor.taskDefsEqual(test.taskDef, taskDef)

    val eventTest: TestDescriptorInternal =
      if !isNestedTest then test.toTestDescriptorInternal else
        TestClassProcessor.verifyCanHaveNestedTest(test, taskDef)
        // TODO [nested] and what if this is a nested suite/class?
        val nestedTest: DefaultTestMethodDescriptor = DefaultTestMethodDescriptor(
          idGenerator.generateId(),
          className,
          TaskDefTest.methodName(selector).get
        )
        // Note: for implied eventTest we reconstruct the 'started' event
        testResultProcessor.started(nestedTest, TestStartEvent(endTime - event.duration, test.id))
        nestedTest

    TestClassProcessor.toOption(event.throwable).foreach((throwable: Throwable) =>
      testResultProcessor.failure(eventTest.getId, ExceptionConverter.convert(throwable))
    )

    testResultProcessor.completed(eventTest.getId, TestCompleteEvent(endTime, TestClassProcessor.toResultType(event.status)))

    isNestedTest

  private def testLogger(test: TaskDefTest): sbt.testing.Logger = new sbt.testing.Logger:
    private def log(logLevel: LogLevel, message: String): Unit = output(
      testId = test.id,
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
      testResultProcessor.failure(test.id, TestFailure.fromTestFrameworkFailure(throwable))

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
    testClassPath: Array[URL],
    testTagsFilter: TestTagsFilter,
    runningInIntelliJIdea: Boolean,
    logLevelEnabled: LogLevel
  ) extends WorkerTestClassProcessorFactory with Serializable:

    override def create(serviceRegistry: ServiceRegistry): TestClassProcessor = TestClassProcessor(
      testClassPath = testClassPath,
      testTagsFilter = testTagsFilter,
      runningInIntelliJIdea = runningInIntelliJIdea,
      logLevelEnabled = logLevelEnabled,
      idGenerator = serviceRegistry.get(classOf[IdGenerator[AnyRef]]),
      clock = serviceRegistry.get(classOf[Clock])
    )

  // Note: Since I can not use the real `rootTestSuiteId` that `DefaultTestExecuter` supplies to the `TestMainAction` -
  // because it is a `String` - and I am not keen on second-guessing what it is anyway,
  // I use a placeholder id and change it to the real one in `FixUpRootTestOutputTestResultProcessor`.
  val rootTestSuiteIdPlaceholder: CompositeId = CompositeId(0L, 0L)

  private def verifyCanHaveNestedTest(test: TaskDefTest, taskDef: TaskDef): Unit =
    require(test.isComposite, "Method tests can not have nested tests")
    val selectors: Array[Selector] = taskDef.selectors
    require(selectors.nonEmpty, s"No selectors in nested TaskDef: $taskDef")
    require(selectors.length == 1, s"More than one selector in nested TaskDef: TaskDef")
    require(!selectors.head.isInstanceOf[TestWildcardSelector], "Encountered TestWildcardSelector!")

  private def taskDefsEqual(left: TaskDef, right: TaskDef): Boolean =
    left.fullyQualifiedName == right.fullyQualifiedName &&
    fingerprintsEqual(left.fingerprint, right.fingerprint) &&
    left.explicitlySpecified == right.explicitlySpecified &&
    left.selectors.length == right.selectors.length &&
    left.selectors.zip(right.selectors).forall((left: Selector, right: Selector) => left.equals(right))

  // Note: I can't rely on all the frameworks providing equals() on their Fingerprint implementations...
  private def fingerprintsEqual(left: Fingerprint, right: Fingerprint): Boolean = (left, right) match
    case (left: AnnotatedFingerprint, right: AnnotatedFingerprint) =>
      left.annotationName == right.annotationName &&
      left.isModule == right.isModule
    case (left: SubclassFingerprint, right: SubclassFingerprint) =>
      left.superclassName == right.superclassName &&
      left.isModule == right.isModule &&
      left.requireNoArgConstructor == right.requireNoArgConstructor
    case _ => false

  private def toOption(optionalThrowable: OptionalThrowable): Option[Throwable] =
    if optionalThrowable.isEmpty then None else Some(optionalThrowable.get)

  private given CanEqual[Status, Status] = CanEqual.derived
  private def toResultType(status: Status): ResultType = status match
    case Status.Success  => ResultType.SUCCESS
    case Status.Error    => ResultType.FAILURE
    case Status.Failure  => ResultType.FAILURE
    case Status.Skipped  => ResultType.SKIPPED
    case Status.Ignored  => ResultType.SKIPPED
    case Status.Canceled => ResultType.SKIPPED
    case Status.Pending  => ResultType.SKIPPED
