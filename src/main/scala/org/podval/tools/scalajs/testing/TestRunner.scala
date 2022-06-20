package org.podval.tools.scalajs.testing

import sbt.testing.{EventHandler, Fingerprint, OptionalThrowable, Runner, Selector, TaskDef, TestSelector,
  Event as TEvent, Status as TStatus, Task as TestTask}
import scala.util.control.NonFatal

// Note: based on sbt.TestRunner from org.scala-sbt.testing
final class TestRunner(
  delegate: Runner,
  listeners: Listeners
):

  final def tasks(testDefs: Set[TestDefinition]): Array[TestTask] = delegate.tasks(
    testDefs
      .map(df => new TaskDef(df.name, df.fingerprint, df.explicitlySpecified, df.selectors))
      .toArray
  )

  final def run(taskDef: TaskDef, testTask: TestTask): (SuiteResult, Seq[TestTask]) =
    val testDefinition: TestDefinition = new TestDefinition(
      taskDef.fullyQualifiedName,
      taskDef.fingerprint,
      taskDef.explicitlySpecified,
      taskDef.selectors
    )
    listeners.debug(s"Running $taskDef")
    val name: String = testDefinition.name

    def runTest(): (SuiteResult, Seq[TestTask]) =
      // here we get the results! here is where we'd pass in the event listener
      val results: scala.collection.mutable.ListBuffer[TEvent] = new scala.collection.mutable.ListBuffer[TEvent]
      val handler: EventHandler = new EventHandler { def handle(e: TEvent): Unit = { results += e } }
      val loggers: Seq[ContentLogger] = listeners.loggers(testDefinition)
      def errorEvents(e: Throwable): Array[sbt.testing.Task] =
        val taskDef: TaskDef = testTask.taskDef
        val event: TEvent = new TEvent:
          override val status: TStatus = TStatus.Error
          override val throwable: OptionalThrowable = new OptionalThrowable(e)
          override val fullyQualifiedName: String = taskDef.fullyQualifiedName
          override val selector: Selector = new TestSelector(name)
          override val fingerprint: Fingerprint = taskDef.fingerprint
          override val duration: Long = -1L

        results += event
        Array.empty

      val nestedTasks: Array[TestTask] =
        try testTask.execute(handler, loggers.map(_.log).toArray)
        catch
          case e: NoClassDefFoundError => errorEvents(e)
          case NonFatal(e)             => errorEvents(e)
          case e: IllegalAccessError   => errorEvents(e)
        finally
          loggers.foreach(_.flush())
      val event: TestEvent = TestEvent(results.toList)
      listeners.safeForeach(_.testEvent(event))
      (SuiteResult(results.toList), nestedTasks.toSeq)

    listeners.safeForeach(_.startGroup(name))
    try
      val (suiteResult, nestedTasks) = runTest()
      listeners.safeForeach(_.endGroup(name, suiteResult.result))
      (suiteResult, nestedTasks)
    catch
      case NonFatal(e) =>
        listeners.safeForeach(_.endGroup(name, e))
        (SuiteResult.Error, Seq.empty[TestTask])
