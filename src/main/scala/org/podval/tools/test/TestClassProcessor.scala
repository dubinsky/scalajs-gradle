package org.podval.tools.test

import org.gradle.api.internal.tasks.testing.{TestClassRunInfo, TestResultProcessor}
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.testing.TestResult.ResultType
import org.gradle.internal.id.{CompositeIdGenerator, IdGenerator, LongIdGenerator}
import org.gradle.internal.time.Clock
import org.opentorah.build.Gradle
import sbt.testing.{Event, EventHandler, Framework, Runner, Status, SuiteSelector, Task, TaskDef, TestSelector}
import java.io.File
import java.net.URLClassLoader
import scala.util.control.NonFatal
import TestResultProcessorEx.*

final class TestClassProcessor(
  testClassPath: Array[File],
  runningInIntelliJIdea: Boolean,
  includeTags: Array[String],
  excludeTags: Array[String],
  clock: Clock
) extends org.gradle.api.internal.tasks.testing.TestClassProcessor:

  private val testClassLoader: ClassLoader =
    if testClassPath == null then null else
    if !TestExecuter.doNotFork
    then URLClassLoader(testClassPath.map(_.toURI.toURL))
    else Gradle.addToClassPath(this, testClassPath.toSeq)

  import TestClassProcessor.FrameworkRun

  private given CanEqual[Framework, Framework] = CanEqual.derived

  private var testResultProcessorOpt: Option[TestResultProcessor] = None
  private def testResultProcessor: TestResultProcessor = testResultProcessorOpt.get

  override def startProcessing(testResultProcessor: TestResultProcessor): Unit = testResultProcessorOpt = Some(
    testResultProcessor
//    if !TestExecuter.doNotFork then testResultProcessor else
//      AttachParentTestResultProcessor(
//        CaptureTestOutputTestResultProcessor(
//          testResultProcessor,
//          JULRedirector()
//        )
//      )
  )

  private var frameworksRuns: Seq[FrameworkRun] = Seq.empty

  private def getRunner(framework: Framework): Runner = synchronized {
    frameworksRuns.find(_.framework == framework).map(_.runner).getOrElse {
      val frameworkDescriptor: FrameworkDescriptor = FrameworkDescriptor.forFramework(framework)

      val args: Array[String] = frameworkDescriptor.args(
        includeTags = includeTags,
        excludeTags = excludeTags,
        isRemote = !TestExecuter.doNotFork
      )

      val runner: Runner = framework.runner(
        if TestExecuter.doNotFork then args else Array.empty,
        args, //if TestExecuter.doNotFork then Array.empty else args,
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
        testId = FrameworkTest.id(frameworkRun.framework),
        message = s"${frameworkRun.framework.name}: $summary",
        logLevel = LogLevel.INFO
      )

  override def processTestClass(testClassRunInfo: TestClassRunInfo): Unit =
    val test: TestClass = testClassRunInfo.asInstanceOf[TestClass]
    val taskDef: TaskDef = TaskDef(
      test.getClassName,
      test.fingerprint,
      test.explicitlySpecified,
      test.selectors.toArray
    )

    val tasks: Array[Task] = getRunner(test.framework).tasks(Array(taskDef))
    require(tasks.nonEmpty, s"Rejected test: $test")
    require(tasks.length == 1, s"Multi-task test: $test")
    run(
      test = test,
      task = tasks.head
    )

  private def run(
    test: TestClass,
    task: Task
  ): Unit =
    val idGenerator: IdGenerator[?] = CompositeIdGenerator(test.getId, new LongIdGenerator)

    // Note: for individual tests, we reconstruct 'started' and 'completed' events
    val eventHandler: EventHandler = (event: Event) =>
      val endTime: Long = clock.getCurrentTime
      require(event.fullyQualifiedName == test.getClassName)
      val testSelector: TestSelector = event.selector.asInstanceOf[TestSelector]
      val method: TestMethod = TestMethod(
        parent = test,
        id = idGenerator.generateId,
        methodName = testSelector.testName,
        selectors = Array(testSelector),
        fingerprint = event.fingerprint
      )
      testResultProcessor.started(
        test = method,
        startTime = endTime - event.duration
      )
      if event.throwable.isDefined then testResultProcessor.failure(
        method.getId,
        event.throwable.get
      )
      testResultProcessor.completed(
        test = method,
        endTime = endTime,
        resultType = TestClassProcessor.fromStatus(event.status)
      )

    val tags: Set[String] = task.tags.toSet

    val allowedByTags: Boolean =
      (includeTags.isEmpty || tags.exists(includeTags.contains)) && !tags.exists(excludeTags.contains)

    if !allowedByTags then
      // skipped test
      testResultProcessor.started(
        test = test,
        startTime = clock.getCurrentTime
      )
      testResultProcessor.completed(
        test = test,
        endTime = clock.getCurrentTime,
        resultType = ResultType.SKIPPED
      )
    else
      testResultProcessor.started(
        test = test,
        startTime = clock.getCurrentTime
      )

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
            testResultProcessor.failure(test.getId, throwable)
            Seq.empty
          finally
            // Note: I think there is no equivalent for this in Gradle...
            ()

        // Node: no idea what nested tasks are; assuming they are Suites.
        for (task: Task, index: Int) <- nestedTasks.zipWithIndex do run(
          task = task,
          test = TestClass(
            parentId = test.getId,
            id = idGenerator.generateId,
            framework = test.framework,
            className = test.getClassName + "-" + index,
            fingerprint = test.fingerprint,
            explicitlySpecified = test.explicitlySpecified,
            selectors = Array(new SuiteSelector)
          )
        )
      finally
        testResultProcessor.completed(
          test = test,
          endTime = clock.getCurrentTime
        )

object TestClassProcessor:

  class FrameworkRun(
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
