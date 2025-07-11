package org.podval.tools.test.run

import org.gradle.api.logging.LogLevel
import org.podval.tools.test.taskdef.{Running, TaskDefs}
import org.podval.tools.util.Scala212Collections.arrayForEach
import sbt.testing.{Logger, Task}
import scala.util.control.NonFatal

final private class RunTestClass(
  val testResultProcessor: TestResultProcessorEx,
  frameworkUsesTestSelectorAsNestedTestSelector: Boolean,
  parentId: AnyRef,
  val running: Running,
  task: Task
):
  def dryRun: Boolean = task.isInstanceOf[DryRunSbtTask]
  def className: String = task.taskDef.fullyQualifiedName
  val testId: AnyRef = testResultProcessor.generateId()
  private val eventHandler: EventHandler = EventHandler(this)

  val logger: Logger = new Logger:
    private def log(logLevel: LogLevel, message: String): Unit = testResultProcessor.output(logLevel, testId, s"sbt $logLevel: $message")
    override def ansiCodesSupported: Boolean = true
    override def error(message: String): Unit = log(LogLevel.ERROR, message)
    override def warn (message: String): Unit = log(LogLevel.WARN , message)
    override def info (message: String): Unit = log(LogLevel.INFO , message)
    override def debug(message: String): Unit = log(LogLevel.DEBUG, message)
    override def trace(throwable: Throwable): Unit = testResultProcessor.failure(testId, throwable)

  def started(
    parentId: AnyRef,
    testId: AnyRef,
    running: Running,
    startTime: Long
  ): Unit =
    val runningEffective: Running =
      if !frameworkUsesTestSelectorAsNestedTestSelector
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

    logger.info(s"RunTestClassProcessor.run(${TaskDefs.toString(task.taskDef)})")

    try
      val nestedTasks: Array[Task] = task.execute(
        eventHandler.handleEvent(_),
        Array(logger)
      )
      arrayForEach(nestedTasks, (nestedTask: Task) =>
        require(nestedTask.taskDef.fullyQualifiedName == className)
        logger.info(s"RunTestClassProcessor: nested task ${TaskDefs.toString(nestedTask.taskDef)}")
      )
      arrayForEach(nestedTasks, (nestedTask: Task) =>
        RunTestClass(
          testResultProcessor,
          frameworkUsesTestSelectorAsNestedTestSelector,
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
