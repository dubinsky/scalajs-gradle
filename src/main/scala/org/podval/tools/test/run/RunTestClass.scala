package org.podval.tools.test.run

import org.gradle.api.logging.LogLevel
import org.podval.tools.test.taskdef.{Selectors, TaskDefs}
import org.podval.tools.platform.Scala212Collections.arrayForEach
import sbt.testing.{Logger, Task}
import scala.util.control.NonFatal

final private class RunTestClass(
  val testResultProcessor: TestResultProcessorEx,
  val frameworkUsesTestSelectorAsNested: Boolean,
  val parentId: AnyRef,
  val selectors: Selectors,
  task: Task
):
  override def toString: String = s"RunTestClass($className, $selectors)"

  def dryRun: Boolean = task.isInstanceOf[DryRunSbtTask]
  def className: String = task.taskDef.fullyQualifiedName
  val testId: AnyRef = testResultProcessor.generateId()
  private val eventHandler: EventHandler = EventHandler(this)

  def debug(message: String): Unit = testResultProcessor.output(
    testId = testId,
    annotation = "backend plugin debugging",
    logLevel = LogLevel.DEBUG,
    message = message
  )

  def started(
    parentId: AnyRef,
    testId: AnyRef,
    selectors: Selectors,
    startTime: Long
  ): Unit = testResultProcessor.started(
    parentId,
    startTime,
    selectors.testDescriptor(testId, className)
  )

  def run(): Unit =
    started(
      parentId = parentId,
      testId = testId,
      selectors = selectors,
      startTime = testResultProcessor.getCurrentTime
    )

    debug(s"RunTestClass.run(${TaskDefs.toString(task.taskDef)})")

    val logger: Logger = new Logger:
      private def log(logLevel: LogLevel, message: String): Unit = testResultProcessor.output(
        testId,
        annotation = "sbt",
        logLevel,
        message
      )
      override def ansiCodesSupported: Boolean = true
      override def error(message: String): Unit = log(LogLevel.ERROR, message)
      override def warn (message: String): Unit = log(LogLevel.WARN , message)
      override def info (message: String): Unit = log(LogLevel.INFO , message)
      override def debug(message: String): Unit = log(LogLevel.DEBUG, message)
      override def trace(throwable: Throwable): Unit = testResultProcessor.failure(testId, throwable)

    try
      val nestedTasks: Array[Task] = task.execute(
        eventHandler.handleEvent(_),
        Array(logger)
      )
      arrayForEach(nestedTasks, (nestedTask: Task) =>
        require(nestedTask.taskDef.fullyQualifiedName == className)
        debug(s"RunTestClass: nested task ${TaskDefs.toString(nestedTask.taskDef)}")
      )
      arrayForEach(nestedTasks, (nestedTask: Task) =>
        RunTestClass(
          testResultProcessor,
          frameworkUsesTestSelectorAsNested,
          parentId = testId,
          selectors = selectors.forNestedTask(nestedTask),
          task = nestedTask
        ).run()
      )
    catch case throwable@(_: NoClassDefFoundError | _: IllegalAccessError | NonFatal(_)) =>
      testResultProcessor.failure(testId, throwable)
    finally
      testResultProcessor.completed(
        testId = testId,
        result = null
      )
