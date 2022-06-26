package org.podval.tools.scalajs

import org.gradle.api.internal.tasks.testing.results.DefaultTestResult
import org.gradle.api.tasks.testing.TestResult
import org.gradle.api.tasks.testing.TestResult.ResultType
import sbt.testing.{Framework, Runner, TestSelector, Event as TEvent, Task as TestTask}
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

// Note: based on sbt.TestRunner from org.scala-sbt.testing
final class TestRunner(
  framework: Framework,
  listeners: TestListeners
):
  private val runner: Runner = framework.runner(
    Array.empty[String],
    Array.empty[String],
    null: ClassLoader
  )

  def toTasks(tests: Seq[TestDefinition]): Seq[Task[Map[String, TestResult]]] =
    val test2tasks: List[(TestDefinition, List[TestTask])] =
      for test: TestDefinition <- tests.toList yield test -> runner.tasks(Array(test.taskDef)).toList

    val rejectedTests: Seq[TestDefinition] =
      for (test: TestDefinition, tasks: List[TestTask]) <- test2tasks if tasks.isEmpty yield test
    require(rejectedTests.isEmpty, s"Rejected tests: $rejectedTests")

    val multiTaskTests: Seq[TestDefinition] =
      for (test: TestDefinition, tasks: List[TestTask]) <- test2tasks if tasks.length > 1 yield test
    require(multiTaskTests.isEmpty, s"Multi-task tests: $rejectedTests")

    for (test: TestDefinition, tasks: List[TestTask]) <- test2tasks yield TestRunnable(
      name = test.taskDef.fullyQualifiedName,
      testDefinition = test,
      testTask = tasks.head,
      runner = this
    ).toTask

  def done(): String = runner.done()

  def run(test: TestDefinition, testTask: TestTask): (TestResult, Seq[TestTask]) =
    val startTime: Long = System.currentTimeMillis
    listeners.startGroup(test, startTime)

    val testEvents: scala.collection.mutable.ListBuffer[TestEvent] = new scala.collection.mutable.ListBuffer[TestEvent]
    def logEvent(testEvent: TestEvent): Unit =
      listeners.testEvent(testEvent)
      testEvents += testEvent

    def suiteResult: TestResult =
      val endTime: Long = System.currentTimeMillis
      val result: TestResult = TestRunner.toTestResult(startTime, endTime, testEvents.toList)
      listeners.endGroup(test, Map(test.taskDef.fullyQualifiedName -> result))
      //  listeners.debug(s"nested: ${nestedTasks.toList}")
      result

    def error(e: Throwable): (TestResult, Seq[TestTask]) =
      logEvent(TestEvent(
        fullyQualifiedName = testTask.taskDef.fullyQualifiedName,
        fingerprint = testTask.taskDef.fingerprint,
        selector = TestSelector(test.taskDef.fullyQualifiedName),
        status = ResultType.FAILURE,
        throwable = Some(e),
        duration = -1L
      ))

      (suiteResult, Seq.empty[TestTask])

    try
      val nestedTasks: Array[TestTask] = testTask.execute(
        (e: TEvent) => logEvent(TestEvent(
          fullyQualifiedName = e.fullyQualifiedName,
          fingerprint = e.fingerprint,
          selector = e.selector,
          status = Tests.toTestResultType(e.status),
          throwable = if e.throwable.isEmpty then None else Some(e.throwable.get),
          duration = e.duration
        )),
        listeners.contentLoggers(test)
      )

      (suiteResult, nestedTasks.toSeq)
    catch
      // TODO use | between patterns?
      case e: NoClassDefFoundError => error(e)
      case NonFatal(e)             => error(e)
      case e: IllegalAccessError   => error(e)
    finally
      listeners.flushContentLoggers(test)

object TestRunner:
  private given CanEqual[ResultType, ResultType] = CanEqual.derived

  private def toTestResult(startTime: Long, endTime: Long, events: Seq[TestEvent]): TestResult =
    def count(status: ResultType): Int = events.count(_.status == status)

    // TODO set TestFailure also...
    DefaultTestResult(
      events.foldLeft(ResultType.SUCCESS)((sum: ResultType, event: TestEvent) => Tests.max(sum, event.status)),
      startTime,
      endTime,
      events.length,
      count(ResultType.SUCCESS),
      count(ResultType.FAILURE),
      events.flatMap(_.throwable).asJava
    )
