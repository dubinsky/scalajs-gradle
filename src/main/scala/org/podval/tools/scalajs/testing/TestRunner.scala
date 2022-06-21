package org.podval.tools.scalajs.testing

import org.gradle.api.tasks.testing.TestResult.ResultType
import sbt.testing.{Event as TEvent, EventHandler, Fingerprint, Framework, OptionalThrowable, Runner, Selector, TaskDef,
  TestSelector, Task as TestTask, Status as TStatus}
import scala.util.control.NonFatal

// Note: based on sbt.TestRunner from org.scala-sbt.testing
// TODO call runner.done!
// TODO are there any ScalaTest parameters that control logging of the test outputs and such?
final class TestRunner(
  framework: Framework,
  listeners: Listeners
):
  private val runner: Runner = framework.runner(
    Array.empty[String],
    Array.empty[String],
    null: ClassLoader
  )

  def toRunnables(tests: Set[TestDefinition]): Seq[TestRunnable] =
    val test2tasks: Map[TestDefinition, List[TestTask]] =
      (for test: TestDefinition <- tests.toList yield test -> runner.tasks(Array(test.taskDef)).toList).toMap

    val rejectedTests: Seq[TestDefinition] =
      for (test: TestDefinition, tasks: List[TestTask]) <- test2tasks.toList if tasks.isEmpty yield test
    require(rejectedTests.isEmpty, s"Rejected tests: $rejectedTests")

    val multiTaskTests: Seq[TestDefinition] =
      for (test: TestDefinition, tasks: List[TestTask]) <- test2tasks.toList if tasks.length > 1 yield test
    require(multiTaskTests.isEmpty, s"Multi-task tests: $rejectedTests")

    val tasks: Seq[TestTask] = test2tasks.toList.flatMap(_._2)
    listeners.debug(s"Tasks for $tests: ${tasks.map(Util.toString)}")

    for testTask: TestTask <- tasks yield TestRunnable(
      name = testTask.taskDef.fullyQualifiedName,
      taskDef = testTask.taskDef,
      testTask = testTask,
      runner = this
    )

  def run(taskDef: TaskDef, testTask: TestTask): (SuiteResult, Seq[TestTask]) =
    listeners.debug(s"Running ${Util.toString(taskDef)}")

    val name: String = taskDef.fullyQualifiedName

    def runTest(): (SuiteResult, Seq[TestTask]) =
      val testEvents: scala.collection.mutable.ListBuffer[TestEvent] = new scala.collection.mutable.ListBuffer[TestEvent]
      def logEvent(testEvent: TestEvent): Unit =
        listeners.safeForeach(_.testEvent(testEvent))
        testEvents += testEvent

      def error(e: Throwable): Array[TestTask] =
        listeners.debug(s"error: ${Util.toString(testTask)} - $e")
        logEvent(new TestEvent(
          fullyQualifiedName = testTask.taskDef.fullyQualifiedName,
          fingerprint = testTask.taskDef.fingerprint,
          selector = new TestSelector(name),
          status = ResultType.FAILURE,
          throwable = new OptionalThrowable(e),
          duration = -1L
        ))
        Array.empty

      val loggers: Seq[ContentLogger] = listeners.loggers(name)

      val nestedTasks: Array[TestTask] =
        try
          listeners.debug(s"execute: ${Util.toString(testTask)}")
          testTask.execute(
            (e: TEvent) => logEvent(TestRunner.toTestEvent(e)),
            loggers.map(_.log).toArray
          )
        catch
          // TODO use | between patterns?
          case e: NoClassDefFoundError => error(e)
          case NonFatal(e)             => error(e)
          case e: IllegalAccessError   => error(e)
        finally
          loggers.foreach(_.flush())

      listeners.debug(s"events: ${testEvents.toList.map(Util.toString)}")
      listeners.debug(s"nested: ${nestedTasks.toList}")

      (SuiteResult(testEvents.toList), nestedTasks.toSeq)

    listeners.safeForeach(_.startGroup(name))
    try
      val (suiteResult: SuiteResult, nestedTasks: Seq[TestTask]) = runTest()
      listeners.safeForeach(_.endGroup(name, suiteResult.result))
      (suiteResult, nestedTasks)
    catch
      case NonFatal(e) =>
        listeners.safeForeach(_.endGroup(name, e))
        (SuiteResult.Error, Seq.empty[TestTask])

object TestRunner:
  def toTestEvent(tEvent: TEvent): TestEvent = TestEvent(
    fullyQualifiedName = tEvent.fullyQualifiedName,
    fingerprint = tEvent.fingerprint,
    selector = tEvent.selector,
    status = toTestResult(tEvent.status),
    throwable = tEvent.throwable,
    duration = tEvent.duration
  )

  given CanEqual[TStatus, TStatus] = CanEqual.derived

  private def toTestResult(status: TStatus): ResultType = status match
    case TStatus.Success  => ResultType.SUCCESS
    case TStatus.Error    => ResultType.FAILURE
    case TStatus.Failure  => ResultType.FAILURE
    case TStatus.Skipped  => ResultType.SKIPPED
    case TStatus.Ignored  => ResultType.SKIPPED
    case TStatus.Canceled => ResultType.SKIPPED
    case TStatus.Pending  => ResultType.SKIPPED
