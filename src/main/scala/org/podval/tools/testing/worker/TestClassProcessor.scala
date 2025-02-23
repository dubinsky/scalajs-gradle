package org.podval.tools.testing.worker

import org.gradle.api.internal.tasks.testing.{DefaultTestClassDescriptor, DefaultTestMethodDescriptor,
  DefaultTestOutputEvent, TestClassRunInfo, TestCompleteEvent, TestDescriptorInternal, TestResultProcessor,
  TestStartEvent, WorkerTestClassProcessorFactory}
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.testing.TestResult.ResultType
import org.gradle.api.tasks.testing.{TestFailure, TestOutputEvent}
import org.gradle.internal.actor.ActorFactory
import org.gradle.internal.id.CompositeIdGenerator.CompositeId
import org.gradle.internal.id.IdGenerator
import org.gradle.internal.time.Clock
import org.podval.tools.testing.exceptions.*
import org.podval.tools.testing.framework.FrameworkDescriptor
import org.podval.tools.testing.serializer.{TaskDefTestSpec, TaskDefTestSpecWriter}
import sbt.testing.{AnnotatedFingerprint, Event, Fingerprint, Framework, NestedSuiteSelector, NestedTestSelector,
  OptionalThrowable, Runner, Selector, Status, SubclassFingerprint, SuiteSelector, Task, TaskDef, TestSelector,
  TestWildcardSelector}
import scala.util.control.NonFatal

final class TestClassProcessor(
  testTagsFilter: TestTagsFilter,
  runningInIntelliJIdea: Boolean,
  logLevelEnabled: LogLevel,
  idGenerator: IdGenerator[?],
  clock: Clock
) extends org.gradle.api.internal.tasks.testing.TestClassProcessor:

  private val reportEvents: Boolean = false

  private var testResultProcessorOpt: Option[TestResultProcessor] = None
  private def testResultProcessor: TestResultProcessor = testResultProcessorOpt.get

  override def startProcessing(testResultProcessor: TestResultProcessor): Unit =
    testResultProcessorOpt = Some(testResultProcessor)

  private var runners: Map[String, Runner] = Map.empty

  private def getRunner(testFramework: Either[String, Framework]): Runner = synchronized:
    val frameworkName: String = testFramework.fold(identity, _.name)
    runners.getOrElse(frameworkName, {
      val runner: Runner = makeRunner(
        testFramework,
        testTagsFilter
      )
      runners = runners.updated(frameworkName, runner)
      runner
    })

  private def makeRunner(
    testFramework: Either[String, Framework],
    testTagsFilter: TestTagsFilter
  ): Runner =
    val framework: Framework = testFramework match
      case Right(framework) =>
        // not forking: just use the framework and its classloader
        framework
      case Left(frameworkName) =>
        // forking: instantiate
        FrameworkDescriptor(frameworkName)
          .newInstance
          .asInstanceOf[Framework]

    val frameworkName: String = framework.name
    val args: Seq[String] = FrameworkDescriptor(frameworkName).args(testTagsFilter)
    if reportEvents then output(s"--- Test framework $frameworkName arguments: $args")
    // Note: we are running the runner in *this* JVM, so remote arguments are not used?
    val remoteArgs: Seq[String] = Seq.empty
    val frameworkClassLoader: ClassLoader = framework.getClass.getClassLoader
    framework.runner(
      args.toArray,
      remoteArgs.toArray,
      frameworkClassLoader
    )

  /**
   * Completes any pending or asynchronous processing. Blocks until all processing is complete.
   */
  override def stop(): Unit =
    for (frameworkName: String, runner: Runner) <- runners.toSeq do
      val summary: String = runner.done()
      output(message = s"$frameworkName summary:\n$summary")

  /**
   * Stops any pending or asynchronous processing immediately.
   * Any test class assigned to this processor, but not yet run will not have results in the output.
   */
  private var stoppedNow: Boolean = false
  override def stopNow(): Unit =
    stoppedNow = true
    stop()

  override def processTestClass(testClassRunInfo: TestClassRunInfo): Unit = if !stoppedNow then
    val taskDefTestSpec: TaskDefTestSpec = TaskDefTestSpecWriter.read(testClassRunInfo)

    // Note: tasks() can take more that one taskDef, but I feed them in one by one:
    val tasks: Array[Task] = getRunner(taskDefTestSpec.framework).tasks(Array(taskDefTestSpec.taskDef))

    // Note: task.taskDef: Returns the TaskDef that was used to request this Task.

    if reportEvents then
      val taskDef: TaskDef = taskDefTestSpec.taskDef
      val (same: Array[Task], different: Array[Task]) =
        tasks.partition((task: Task) => TestClassProcessor.taskDefsEqual(taskDef, task.taskDef))

      def out(message: String): Unit =
        val frameworkName: String = taskDefTestSpec.framework.fold(identity, _.name)
        output(s"--- $frameworkName $message")

      val taskDefString: String = TestClassProcessor.toString(taskDef)
      if same.length > 1 then out(s"MULTIPLE TASKS FOR THE $taskDefString") else
        if different.isEmpty then
          if same.isEmpty
          then out(s"REJECTED $taskDefString")
          else out(s"ACCEPTED $taskDefString")
        else
          val differentStr: String = different.map(_.taskDef).map(TestClassProcessor.toString).mkString("\n")
          if same.isEmpty
          then out(s"REPLACED $taskDefString with: $differentStr")
          else out(s"ADDED TO $taskDefString: $differentStr")

    for task: Task <- tasks do
      val selector: Selector = TestClassProcessor.getSelector(task.taskDef)
      run(
        parentId = null,
        selector = if TestClassProcessor.isMethod(selector) then new SuiteSelector else selector,
        task = task
      )

  private def run(
    parentId: AnyRef,
    selector: Selector,
    task: Task
  ): Unit =
    val startTime: Long = clock.getCurrentTime
    var isTestCompleted: Boolean = false

    val testId: AnyRef = idGenerator.generateId()
    val className: String = task.taskDef.fullyQualifiedName
    if reportEvents then output(s"--- TestClassProcessor.run($className, $selector)")
    testResultProcessor.started(
      TestClassProcessor.testDescriptorInternal(testId, className, selector),
      TestStartEvent(startTime, parentId)
    )

    try
      val nestedTasks: Seq[Task] =
        try
          if reportEvents then output(s"--- Task(${TestClassProcessor.toString(task.taskDef)}, ${task.tags.mkString}).execute()")
          task.execute(
            (event: Event) =>
              require(!isTestCompleted, s"Received event for a completed test ${TestClassProcessor.toString(task.taskDef)}")
              isTestCompleted = handleEvent(
                testId,
                className,
                selector,
                event
              ),
            Array(testLogger(testId))
          ).toSeq
        catch case throwable@(_: NoClassDefFoundError | _: IllegalAccessError | NonFatal(_)) =>
          testResultProcessor.failure(
            testId,
            TestFailure.fromTestFrameworkFailure(throwable)
          )
          Seq.empty

      for nestedTask: Task <- nestedTasks do
        if reportEvents then output(s"----- GOT NESTED TASK FOR $className $selector: ${TestClassProcessor.toString(nestedTask.taskDef)}")
        run(
          parentId = testId,
          selector = TestClassProcessor.verifyCanNest(selector, TestClassProcessor.getSelector(nestedTask.taskDef)),
          task = nestedTask
        )
    finally
      if !isTestCompleted then testResultProcessor.completed(
        testId,
        TestCompleteEvent(clock.getCurrentTime)
      )

  // Although it is tempting to help the test frameworks out by filtering tests based on their tags
  // right here, using code like this:
  //    val isAllowed: Boolean = true
  //      val tags: Array[String] = task.tags
  //      if tags.nonEmpty then output(s"--- got tags for $className $selector: " + tags.mkString("[", ", ", "]"))
  //      def isListed(list: Array[String]): Boolean = tags.exists(list.contains)
  //      (testTagsFilter.include.isEmpty || isListed(testTagsFilter.include)) && !isListed(testTagsFilter.exclude)
  //    if !isAllowed then
  //      // skipped test
  //      testResultProcessor.completed(testId, TestCompleteEvent(startTime, ResultType.SKIPPED))
  //    else ...
  //
  // - it is unnecessary, since all the test frameworks plugin supports that support tagging accept
  //   arguments that allow them to do the filtering internally;
  // - it is destructive, since none of the test frameworks plugin supports populate `task.tags`,
  //   so with explicit tag inclusions, none of the tests run!

  private def handleEvent(
    testId: AnyRef,
    className: String,
    selector: Selector,
    event: Event
  ): Boolean =
    val endTime: Long = clock.getCurrentTime
    val resultType: ResultType = TestClassProcessor.toResultType(event.status)
    // Note: Assuming that if the event is for the testId, selector is the same,
    // and if not - it is a *test method* in the same class.
    val isSameTest: Boolean = TestClassProcessor.selectorsEqual(selector, event.selector)
    val throwableOpt: Option[Throwable] =
      if event.throwable.isEmpty
      then None
      else Some(event.throwable.get)
      
    if reportEvents then output(s"----- GOT EVENT FOR $className $selector: ${event.fullyQualifiedName} ${event.selector} $resultType; throwable=$throwableOpt isSameTest=$isSameTest")
    
    val eventTestId: AnyRef = if isSameTest then testId else
      val nestedTestId: AnyRef = idGenerator.generateId()
      // Note: reconstruct implied 'started' event
      testResultProcessor.started(
        TestClassProcessor.testDescriptorInternal(
          testId = nestedTestId, 
          className = className,
          selector = TestClassProcessor.verifyCanNest(selector, event.selector)
        ),
        TestStartEvent(endTime - event.duration, testId)
      )
      nestedTestId

    throwableOpt
      .orElse:
        // It is possible, albeit not nice, for the test framework to not populate the `event.throwable`
        // of the `Failure` event; Scala.js's JUnit does this.
        // Gradle, on the other hand, treats a test as failed only when it receives a `throwable` for the test -
        // otherwise, although XML report does record the failure, HTML report does not,
        // nor does Gradle build fail.
        // This is why I supply a synthesized event for
        // *method* failures if one did not come up from the framework.
        given CanEqual[ResultType, ResultType] = CanEqual.derived
        if (resultType != ResultType.FAILURE) || isSameTest then None else Some: // TODO use !isMethod() instead of isSameTest?
          val message = "A FAKE EXCEPTION TO MAKE Gradle FEEL THE TEST FAILURE"
          if reportEvents then output(s"----- MAKING UP $message")
          Exception(message)
      .foreach: (throwable: Throwable) =>
        testResultProcessor.failure(
          eventTestId,
          exceptionConverter(throwable.getClass.getName).toTestFailure(throwable)
        )

    testResultProcessor.completed(
      eventTestId,
      TestCompleteEvent(endTime, resultType)
    )

    // Returns true if the test completed
    isSameTest

  private def exceptionConverter(throwableClassName: String): ExceptionConverter = throwableClassName match
    case "org.junit.ComparisonFailure" => // JUnit
      OrgJUnitComparisonFailureConverter
    case "junit.framework.ComparisonFailure" => // JUnit
      JUnitFrameworkComparisonFailureConverter
    case "munit.ComparisonFailException" =>  // MUnit
      MUnitComparisonFailExceptionConverter
    case "org.scalatest.exceptions.TestFailedException" => // ScalaTest
      OrgScalaTestExceptionsTestFailedExceptionConverter
    case "utest.AssertionError" => // UTest
      UTestAssertionErrorConverter
    case "java.lang.AssertionError" =>
      JavaLangAssertionErrorConverter  
    case "java.lang.Exception" => 
      DefaultExceptionConverter
    case "org.scalajs.testing.common.Serializer$ThrowableSerializer$$anon$3" => // Scala.js
      DefaultExceptionConverter
    case _ => // Everything else (there is no framework-specific exceptions for ScalaCheck, specs2 nor ZIO Test):
      output(s"--- Unknown Throwable class name: $throwableClassName")
      DefaultExceptionConverter

  private def testLogger(testId: AnyRef): sbt.testing.Logger = new sbt.testing.Logger:
    private def log(logLevel: LogLevel, message: String): Unit = output(
      testId = testId,
      message = message,
      logLevel = logLevel
    )

    override def ansiCodesSupported: Boolean = true
    override def error(message: String): Unit = log(LogLevel.ERROR, message)
    override def warn (message: String): Unit = log(LogLevel.WARN , message)
    override def info (message: String): Unit = log(LogLevel.INFO , message)
    override def debug(message: String): Unit = log(LogLevel.DEBUG, message)
    override def trace(throwable: Throwable): Unit =
      testResultProcessor.failure(
        testId,
        TestFailure.fromTestFrameworkFailure(throwable)
      )

  private def output(message: String): Unit = output(
    testId = TestClassProcessor.rootTestSuiteIdPlaceholder,
    logLevel = LogLevel.LIFECYCLE,
    message = message
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

object TestClassProcessor:
  // Note: Since I can not use the real `rootTestSuiteId` that `DefaultTestExecuter` supplies to the `TestMainAction` -
  // because it is a `String` - and I am not keen on second-guessing what it is anyway,
  // I use a placeholder id and change it to the real one in `FixUpRootTestOutputTestResultProcessor`.
  val rootTestSuiteIdPlaceholder: CompositeId = CompositeId(0L, 0L)
  
  // TODO move to TestFramework?
  final class Factory(
    testTagsFilter: TestTagsFilter,
    runningInIntelliJIdea: Boolean,
    logLevelEnabled: LogLevel
  ) extends WorkerTestClassProcessorFactory with Serializable:

    override def create(
      idGenerator: IdGenerator[?],
      actorFactory: ActorFactory,
      clock: Clock
    ): TestClassProcessor = TestClassProcessor(
      testTagsFilter = testTagsFilter,
      runningInIntelliJIdea = runningInIntelliJIdea,
      logLevelEnabled = logLevelEnabled,
      idGenerator = idGenerator,
      clock = clock
    )

  private def getSelector(taskDef: TaskDef): Selector =
    require(taskDef.selectors.length == 1, "Exactly one Selector is required")
    taskDef.selectors.head

  // Note: in reality, an individual test is not always a method (e.g., in ScalaTest), but compared to a class it is :)
  private def verifyCanNest(selector: Selector, nestedSelector: Selector): Selector =
    require(!TestClassProcessor.isMethod(selector), s"Method tests like $selector can not have nested tests like $nestedSelector")
    require(TestClassProcessor.isMethod(nestedSelector), s"Only method tests can be nested, not $nestedSelector")
    nestedSelector

  private def testDescriptorInternal(
    testId: AnyRef,
    className: String,
    selector: Selector,
  ): TestDescriptorInternal =
    // MUnit and JUnit4 set both the event's fullyQualifiedName and the selector
    // to something like <class name>.<method name>, which confuses Gradle into thinking
    // that that is a new class name, since the event fingerprint says so...
    val methodName: Option[String] = selector match
      case testSelector: TestSelector => Some(testSelector.testName)
      case nestedTestSelector: NestedTestSelector => Some(nestedTestSelector.testName)
      case _ => None

    methodName match
      case None => DefaultTestClassDescriptor(testId, className)
      case Some(methodName) => DefaultTestMethodDescriptor(testId, className, methodName.stripPrefix(className + "."))

  private def isMethod(selector: Selector): Boolean = selector match
    case _: TestSelector | _: NestedTestSelector | _: TestWildcardSelector => true
    case _ => false

  private def taskDefsEqual(left: TaskDef, right: TaskDef): Boolean =
    left.fullyQualifiedName == right.fullyQualifiedName &&
    fingerprintsEqual(left.fingerprint, right.fingerprint) &&
    left.explicitlySpecified == right.explicitlySpecified &&
    left.selectors.length == right.selectors.length &&
    left.selectors.zip(right.selectors).forall((left: Selector, right: Selector) => selectorsEqual(left, right))

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

  // Selector subclasses are final and override equals(),
  // so `left.equals(right)` should work just fine,
  // but with ScalaCheck running on ScalaJS (but not plain Scala)
  // I get TestSelector(String.startsWith) != TestSelector(String.startsWith) -
  // for every test method, even other than `String.startsWith`, so...
  private def selectorsEqual(left: Selector, right: Selector): Boolean =
    val result: Boolean = (left, right) match
      case (_: SuiteSelector, _: SuiteSelector) => true
      case (left: NestedSuiteSelector, right: NestedSuiteSelector) => left.suiteId == right.suiteId
      case (left: TestSelector, right: TestSelector) => left.testName == right.testName
      case (left: NestedTestSelector, right: NestedTestSelector) => (left.suiteId == right.suiteId) && left.testName == right.testName()
      case (left: TestWildcardSelector, right: TestWildcardSelector) => left.testWildcard == right.testWildcard
      case _ => false

//    require(result == left.equals(right),
//      s"--- SELECTOR COMPARISON DISCREPANCY: $left [${left.getClass}] and $right [${right.getClass}]"
//    )

    result

  def toString(taskDef: TaskDef): String =
    def className(isModule: Boolean): String = taskDef.fullyQualifiedName + (if isModule then "$" else "")

    val name: String = taskDef.fingerprint match
      case annotated: AnnotatedFingerprint => s"@${annotated.annotationName} ${className(annotated.isModule)}"
      case subclass : SubclassFingerprint  => s"${className(subclass.isModule)} extends ${subclass.superclassName}"

    val selectors: String = taskDef.selectors.map(_.toString).mkString("[", ", ", "]")

    s"$name selectors=$selectors explicitlySpecified=${taskDef.explicitlySpecified}"

  private def toResultType(status: Status): ResultType =
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

