package org.podval.tools.test.run

import org.gradle.api.internal.tasks.testing.{TestClassProcessor, TestClassRunInfo, TestResultProcessor}
import org.gradle.api.logging.LogLevel
import org.gradle.internal.id.CompositeIdGenerator.CompositeId
import org.gradle.internal.id.IdGenerator
import org.gradle.internal.time.Clock
import org.podval.tools.test.framework.FrameworkProvider
import org.podval.tools.test.taskdef.{Running, TaskDefs, TestClassRun}
import org.podval.tools.util.Scala212Collections.{arrayAppend, arrayFind, arrayForEach}
import sbt.testing.{Framework, Runner, Task, TaskDef}

object RunTestClassProcessor:
  val rootTestSuiteIdPlaceholder: CompositeId = CompositeId(0L, 0L)

final class RunTestClassProcessor(
  includeTags: Array[String],
  excludeTags: Array[String],
  logLevelEnabled: LogLevel,
  isRunningInIntelliJ: Boolean,
  dryRun: Boolean,
  idGenerator: IdGenerator[?],
  clock: Clock
) extends TestClassProcessor:

  private var testResultProcessorOpt: Option[TestResultProcessor] = None

  override def startProcessing(testResultProcessor: TestResultProcessor): Unit =
    testResultProcessorOpt = Some(testResultProcessor)

  private lazy val testResultProcessor: TestResultProcessorEx = TestResultProcessorEx(
    testResultProcessorOpt.get,
    logLevelEnabled,
    clock,
    idGenerator
  )

  private var runners: Array[(String, Runner)] = Array.empty

  private def getRunner(frameworkProvider: FrameworkProvider): Runner = synchronized:
    val frameworkName: String = frameworkProvider.frameworkName
    arrayFind(runners, _._1 == frameworkName).map(_._2).getOrElse:
      val runner: Runner = frameworkProvider.runner(
        isRunningInIntelliJ = isRunningInIntelliJ,
        includeTags = includeTags,
        excludeTags = excludeTags
      )
      runners = arrayAppend(runners, (frameworkName, runner))
      runner
  
  override def stop(): Unit = arrayForEach(runners, (frameworkName, runner) =>
    testResultProcessor.output(
      LogLevel.INFO,
      RunTestClassProcessor.rootTestSuiteIdPlaceholder,
      s"RunTestClassProcessor $frameworkName summary:\n${runner.done}"
    )
  )

  private var stoppedNow: Boolean = false

  override def stopNow(): Unit =
    stoppedNow = true
    stop()

  override def processTestClass(testClassRunInfo: TestClassRunInfo): Unit = if !stoppedNow then
    val testClassRun: TestClassRun = testClassRunInfo.asInstanceOf[TestClassRun]
    val running: Running = Running.forTestClassRun(testClassRun)

    val taskDef: TaskDef = TaskDef(
      testClassRun.getTestClassName,
      testClassRun.fingerprint,
      testClassRun.explicitlySpecified,
      running.selectors
    )

    val tasks: Array[Task] =
      if dryRun
      then Array(DryRunSbtTask(taskDef))
      else getRunner(testClassRun.frameworkProvider).tasks(Array(taskDef))

    require(tasks.length == 1)
    val task: Task = tasks(0)
    require(TaskDefs.equal(task.taskDef, taskDef))

    RunTestClass(
      testResultProcessor = testResultProcessor,
      frameworkUsesTestSelectorAsNestedTestSelector =
        testClassRun.frameworkProvider.frameworkDescriptor.usesTestSelectorAsNestedTestSelector,
      parentId = null,
      running = running,
      task = task
    ).run()
