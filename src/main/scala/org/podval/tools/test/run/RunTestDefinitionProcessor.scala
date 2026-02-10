package org.podval.tools.test.run

import org.gradle.api.internal.tasks.testing.{TestDefinition, TestDefinitionProcessor, TestResultProcessor}
import org.gradle.api.logging.LogLevel
import org.gradle.internal.id.CompositeIdGenerator.CompositeId
import org.gradle.internal.id.IdGenerator
import org.gradle.internal.time.Clock
import org.podval.tools.build.{Output, TestFramework}
import org.podval.tools.util.Scala212Collections.{arrayAppend, arrayConcat, arrayFind, arrayForEach}
import sbt.testing.{Runner, Task, TaskDef}

object RunTestDefinitionProcessor:
  val rootTestSuiteIdPlaceholder: CompositeId = CompositeId(0L, 0L)

final class RunTestDefinitionProcessor[D <: TestDefinition](
  includeTags: Array[String],
  excludeTags: Array[String],
  output: Output,
  dryRun: Boolean,
  idGenerator: IdGenerator[?],
  clock: Clock
) extends TestDefinitionProcessor[D]:

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

  private def getRunner(framework: TestFramework.Loaded): Runner = synchronized:
    arrayFind(runners, _._1 == framework.nameSbt).map(_._2).getOrElse:
      val args: Array[String] = arrayConcat(
        framework.framework.additionalOptions,
        framework.framework.tagOptions.map(_.args(includeTags, excludeTags)).getOrElse(Array.empty)
      )
      val runner: Runner = framework.runner(args)
      runners = arrayAppend(runners, (framework.nameSbt, runner))
      runner

  override def stop(): Unit = arrayForEach(runners, (frameworkName, runner: Runner) =>
    val summary: String = runner.done
    if !summary.isBlank then testResultProcessor.output(
      testId = RunTestDefinitionProcessor.rootTestSuiteIdPlaceholder,
      annotation = "test framework summary",
      logLevel = LogLevel.INFO,
      message = s"[$frameworkName]\n$summary"
    )
  )

  private var stoppedNow: Boolean = false

  override def stopNow(): Unit =
    stoppedNow = true
    stop()

  override def processTestDefinition(testDefinition: D): Unit = if !stoppedNow then
    val testClassRun: TestClassRun = testDefinition.asInstanceOf[TestClassRun]
    val selectors: Selectors = Selectors(testClassRun.testNames, testClassRun.testWildcards)
    val taskDef: TaskDef = TaskDef(
      testClassRun.className,
      testClassRun.fingerprint,
      testClassRun.explicitlySpecified,
      selectors.selectors
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
      selectors = selectors,
      task = task
    ).run()
