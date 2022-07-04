package org.podval.tools.test

import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.testing.TestResult.ResultType
import org.gradle.internal.id.{CompositeIdGenerator, IdGenerator, LongIdGenerator}
import sbt.testing.{Event, EventHandler, Framework, Runner, Status, Task, TestSelector}
import java.io.File
import scala.util.control.NonFatal

final class TestRunner(
  parentId: Object,
  idGenerator: IdGenerator[?],
  framework: Framework,
  testClassLoader: ClassLoader,
  listeners: TestListeners
):

  private val frameworkTest: TestDescriptor.Suite =
    TestDescriptor.Synthetic(idGenerator.generateId, s"${framework.name} tests")

  private val runner: Runner = framework.runner(
    Array.empty[String],
    Array.empty[String],
    testClassLoader
  )

  def start(): Unit =
    listeners.suiteStarted(parentId = parentId, frameworkTest)

  def runClass(test: TestDescriptor.Class): Unit = runClass(
    parentId = frameworkTest.getId,
    test = test.withId(idGenerator.generateId),
    task = None
  )

  def close(): Unit =
    // Note: summary is ignored
    val summary: String = runner.done()
    listeners.suiteCompleted(frameworkTest)

  private def runClass(
    parentId: Object,
    test: TestDescriptor.Class,
    task: Option[Task]
  ): Unit =
    listeners.suiteStarted(parentId = parentId, test)

    require(task.isEmpty || test.includeMethods.isEmpty)

    if test.includeMethods.isEmpty then run(
      test = test,
      task = task
    ) else for includeTest: String <- test.includeMethods do runMethod(
      parentId = test.getId,
      method = test.method(
        id = idGenerator.generateId,
        methodName = includeTest
      )
    )

    listeners.suiteCompleted(test)

  private def runMethod(
    parentId: Object,
    method: TestDescriptor.Method
  ): Unit =
    // TODO when explicitly specified test is commented out, it shows as "passed"!
    listeners.methodStarted(parentId = parentId, test = method, startTime = System.currentTimeMillis)
    run(
      test = method,
      task = None
    )
    listeners.methodCompleted(test = method, endTime = System.currentTimeMillis)

  private def run(
    test: TestDescriptor.WithTaskDef,
    task: Option[Task]
  ): Unit =
    // TODO switch to TestFailure after Gradle 7.6
    def eventHandler: EventHandler = (event: Event) =>
      val endTime: Long = System.currentTimeMillis
      val duration: Long = event.duration
      val startTime: Long = endTime - duration
      val className: String = event.fullyQualifiedName
      require(className == test.getClassName)
      val methodName: String = event.selector.asInstanceOf[TestSelector].testName // TODO could be NestedSuiteSelector...
      //val resultType: ResultType = TestRunner.fromStatus(event.status)
      //listeners.log(s"methodName=$methodName; result type=$resultType")
      val throwable: Option[Throwable] = if event.throwable.isEmpty then None else Some(event.throwable.get)

      // Note: for individual tests, we reconstruct 'started' and 'completed' events
      val (reconstruct: Boolean, method: TestDescriptor.Method) = test match
        case method: TestDescriptor.Method => (false, method)
        case clazz : TestDescriptor.Class  => (true , clazz.method(
          id = idGenerator.generateId,
          methodName = methodName
        ))

      // TODO add more required()s (fingerprint, selector)
      require(methodName == method.getName)
      if reconstruct then listeners.methodStarted(parentId = test.getId, test = method, startTime = startTime)
      throwable.foreach((throwable: Throwable) => listeners.methodFailed(test = method, throwable = throwable))
      if reconstruct then listeners.methodCompleted(method, endTime = endTime)

    val nestedTasks: Seq[Task] =
      try task
        .getOrElse {
          val tasks: Array[Task] = runner.tasks(Array(test.taskDef))
          require(tasks.nonEmpty, s"Rejected test: $test")
          require(tasks.length == 1, s"Multi-task test: $test")
          tasks.head
        }
        .execute(
          eventHandler,
          listeners.contentLoggers(s"${framework.name}: ")
        )
        .toSeq
      catch case throwable@(_: NoClassDefFoundError | _: IllegalAccessError | NonFatal(_)) =>
        listeners.error(test, throwable)
        Seq.empty
      finally
        listeners.flushContentLoggers(test)

    for (nestedTask: Task, index: Int) <- nestedTasks.zipWithIndex do
      listeners.log(s"NESTED TEST TASK: $nestedTask")
      // TODO no idea what nested tasks are; assuming they are Suites.
      runClass(
        parentId = test.getId,
        test = test.withIndex(idGenerator.generateId, index),
        task = Some(nestedTask)
      )

object TestRunner:

  def run(
    testEnvironment: TestEnvironment,
    analysisFile: File,
    includePatterns: Set[String],
    excludePatterns: Set[String],
    commandLineIncludePatterns: Set[String],
    testResultProcessor: TestResultProcessor,
    logger: Logger,
    useColours: Boolean
  ): Unit =
    val listeners: TestListeners = TestListeners(
      testResultProcessor = testResultProcessor,
      sourceMapper = testEnvironment.sourceMapper,
      logger = logger,
      useColours = useColours
    )

    val testDiscovery: TestDiscovery = TestDiscovery(
      loadedFrameworks = testEnvironment.loadAllFrameworks,
      analysisFile = analysisFile,
      testFiltering = TestFiltering(
        includePatterns,
        excludePatterns,
        commandLineIncludePatterns
      )
    )

    if testDiscovery.tests.nonEmpty then
      val overallId: Object = "sbt"
      val overall: TestDescriptor.Suite = TestDescriptor.Synthetic(overallId, "SBT Tests")
      listeners.suiteStarted(parentId = null, overall)

      for (framework: Framework, tests: Seq[TestDescriptor.Class]) <- testDiscovery.tests do
        val testRunner: TestRunner = TestRunner(
          listeners = listeners,
          testClassLoader = testEnvironment.testClassLoader,
          framework = framework,
          parentId = overallId,
          idGenerator = CompositeIdGenerator(
            CompositeIdGenerator.CompositeId(
              overallId,
              framework.name
            ),
            new LongIdGenerator
          )
        )
        testRunner.start()
        tests.foreach(testRunner.runClass)
        testRunner.close()

      listeners.suiteCompleted(overall)

    // TODO gets called too early?!
    //  testEnvironment.close()
    // TODO stop the thread in TestListeners

  private given CanEqual[Status, Status] = CanEqual.derived
  private def fromStatus(status: Status): ResultType = status match
    case Status.Success  => ResultType.SUCCESS
    case Status.Error    => ResultType.FAILURE
    case Status.Failure  => ResultType.FAILURE
    case Status.Skipped  => ResultType.SKIPPED
    case Status.Ignored  => ResultType.SKIPPED
    case Status.Canceled => ResultType.SKIPPED
    case Status.Pending  => ResultType.SKIPPED
