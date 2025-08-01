package org.podval.tools.test.run

import org.gradle.api.internal.tasks.testing.{TestClassProcessor, TestClassRunInfo, TestResultProcessor}
import org.gradle.api.logging.LogLevel
import org.gradle.internal.id.CompositeIdGenerator.CompositeId
import org.gradle.internal.id.IdGenerator
import org.gradle.internal.time.Clock
import org.podval.tools.platform.Output
import org.podval.tools.test.framework.Framework
import org.podval.tools.test.taskdef.{Running, TaskDefs, TestClassRun}
import org.podval.tools.util.Scala212Collections.{arrayAppend, arrayConcat, arrayFind, arrayForEach}
import sbt.testing.{Runner, Task, TaskDef}

object RunTestClassProcessor:
  val rootTestSuiteIdPlaceholder: CompositeId = CompositeId(0L, 0L)

final class RunTestClassProcessor(
  includeTags: Array[String],
  excludeTags: Array[String],
  output: Output,
  dryRun: Boolean,
  idGenerator: IdGenerator[?],
  clock: Clock
) extends TestClassProcessor:

  private var testResultProcessorOpt: Option[TestResultProcessor] = None

  override def startProcessing(testResultProcessor: TestResultProcessor): Unit =
    testResultProcessorOpt = Some(testResultProcessor)

  private lazy val testResultProcessor: TestResultProcessorEx = TestResultProcessorEx(
    testResultProcessorOpt.get,
    output,
    clock,
    idGenerator
  )

  private var runners: Array[(String, Runner)] = Array.empty

  private def getRunner(framework: Framework.Loaded): Runner = synchronized:
    arrayFind(runners, _._1 == framework.name).map(_._2).getOrElse:
      val args: Array[String] = arrayConcat(
        framework.framework.additionalOptions,
        framework.framework.tagOptions.map(_.args(includeTags, excludeTags)).getOrElse(Array.empty)
      )
      val runner: Runner = framework.runner(args)
      runners = arrayAppend(runners, (framework.name, runner))
      runner

  override def stop(): Unit = arrayForEach(runners, (frameworkName, runner: Runner) =>
    val summary: String = runner.done
    val isSummaryMeaningful: Boolean =
      !summary.isBlank &&
      // TODO remove
      summary != "Completed tests" && // dummy ZIO Test summary on JVM
      summary != "Done"  // dummy ZIO Test summary on Scala.js and Scala Native
    if isSummaryMeaningful then testResultProcessor.output(
      testId = RunTestClassProcessor.rootTestSuiteIdPlaceholder,
      annotation = "test framework summary",
      logLevel = LogLevel.INFO,
      message = s"[$frameworkName]\n$summary"
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
      else getRunner(testClassRun.framework).tasks(Array(taskDef))

    require(tasks.length == 1)
    val task: Task = tasks(0)
    require(TaskDefs.equal(task.taskDef, taskDef))

    RunTestClass(
      testResultProcessor = testResultProcessor,
      frameworkUsesTestSelectorAsNested = testClassRun.framework.framework.usesTestSelectorAsNested,
      parentId = null,
      running = running,
      task = task
    ).run()
