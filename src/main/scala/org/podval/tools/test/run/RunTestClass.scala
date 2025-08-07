package org.podval.tools.test.run

import org.gradle.api.logging.LogLevel
import org.podval.tools.platform.Output
import org.podval.tools.test.taskdef.{Running, TaskDefs}
import org.podval.tools.platform.Scala212Collections.arrayForEach
import sbt.testing.{Logger, Task}
import scala.util.control.NonFatal

final private class RunTestClass(
  val testResultProcessor: TestResultProcessorEx,
  frameworkUsesTestSelectorAsNested: Boolean,
  parentId: AnyRef,
  val running: Running,
  task: Task
):
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
    running: Running,
    startTime: Long
  ): Unit =
    val runningEffective: Running =
      if !frameworkUsesTestSelectorAsNested
      then running
      else running.reconstructNestedTestSelector
    
    testResultProcessor.started(
      parentId,
      testId,
      testClassName = runningEffective.suiteId.getOrElse(className),
      testName = runningEffective.testName,
      startTime
    )

  def run(): Unit =
    started(
      parentId = parentId,
      testId = testId,
      running = running,
      startTime = testResultProcessor.getCurrentTime
    )

    debug(s"RunTestClassProcessor.run(${TaskDefs.toString(task.taskDef)})")

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
        debug(s"RunTestClassProcessor: nested task ${TaskDefs.toString(nestedTask.taskDef)}")
      )
      arrayForEach(nestedTasks, (nestedTask: Task) =>
        RunTestClass(
          testResultProcessor,
          frameworkUsesTestSelectorAsNested,
          parentId = testId,
          running = running.forNestedTask(nestedTask),
          task = nestedTask
        ).run()
      )
    catch case throwable@(_: NoClassDefFoundError | _: IllegalAccessError | NonFatal(_)) =>
      testResultProcessor.failure(testId, throwable)
    finally
      testResultProcessor.completed(
        testId = testId,
        endTime = testResultProcessor.getCurrentTime,
        result = null
      )
