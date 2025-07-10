package org.podval.tools.test.task

import org.gradle.api.Action
import org.gradle.api.internal.classpath.ModuleRegistry
import org.gradle.api.internal.tasks.testing.{JvmTestExecutionSpec, TestClassProcessor, TestResultProcessor,
  WorkerTestClassProcessorFactory}
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.internal.tasks.testing.worker.ForkedTestClasspath
import org.gradle.internal.actor.ActorFactory
import org.gradle.internal.time.Clock
import org.gradle.internal.work.WorkerLeaseService
import org.gradle.process.JavaForkOptions
import org.gradle.process.internal.worker.{WorkerProcessBuilder, WorkerProcessFactory}
import org.podval.tools.build.SourceMapper
import org.podval.tools.test.run.{FixRootTestSuiteOutputTestResultProcessor, RunTestClassProcessorFactory,
  SourceMappingTestResultProcessor, TracingTestResultProcessor, WriteTestClassProcessor}

class TestExecuter(
  testsCanNotBeForked: Boolean,
  sourceMapper: Option[SourceMapper],
  testFilter: DefaultTestFilter,
  maxWorkerCount: Int,
  clock: Clock,
  workerProcessFactory: WorkerProcessFactory,
  actorFactory: ActorFactory,
  workerLeaseService: WorkerLeaseService,
  moduleRegistry: ModuleRegistry
) extends DefaultTestExecuter(
  workerProcessFactory,
  actorFactory,
  moduleRegistry,
  workerLeaseService,
  maxWorkerCount,
  clock,
  testFilter
):
  override def execute(
    testExecutionSpec: JvmTestExecutionSpec,
    testResultProcessor: TestResultProcessor
  ): Unit =
    // Deeper down, TestMainAction wraps testResultProcessorEffective in AttachParentTestResultProcessor.
    val testResultProcessorEffective: TestResultProcessor =
      FixRootTestSuiteOutputTestResultProcessor(
        SourceMappingTestResultProcessor(
          TracingTestResultProcessor(
            testResultProcessor
          ),
          sourceMapper
        )
      )

    super.execute(testExecutionSpec, testResultProcessorEffective)

  override protected def createTestClassProcessor(
    workerLeaseService: WorkerLeaseService,
    workerProcessFactory: WorkerProcessFactory,
    workerTestClassProcessorFactory: WorkerTestClassProcessorFactory,
    javaForkOptions: JavaForkOptions,
    classpath: ForkedTestClasspath,
    workerConfigurationAction: Action[WorkerProcessBuilder]
  ): TestClassProcessor = 
    if testsCanNotBeForked then
      workerTestClassProcessorFactory.asInstanceOf[RunTestClassProcessorFactory].createNonForking(clock)
    else
      // WriteTestClassProcessor is at the end of the TestClassProcessor chain created in DefaultTestExecuter,
      // right before the ForkingTestClassProcessor,
      // so that PatternMatchTestClassProcessor and RunPreviousFailedFirstTestClassProcessor can do their jobs.
      WriteTestClassProcessor(
        super.createTestClassProcessor(
          workerLeaseService,
          workerProcessFactory,
          workerTestClassProcessorFactory,
          javaForkOptions,
          classpath,
          workerConfigurationAction
        )
      )
