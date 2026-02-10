package org.podval.tools.test.run

import org.gradle.api.Action
import org.gradle.api.internal.classpath.ModuleRegistry
import org.gradle.api.internal.tasks.testing.{JvmTestExecutionSpec, TestDefinition, TestDefinitionProcessor,
  TestResultProcessor, WorkerTestDefinitionProcessorFactory}
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.internal.tasks.testing.worker.ForkedTestClasspath
import org.gradle.internal.actor.ActorFactory
import org.gradle.internal.time.Clock
import org.gradle.internal.work.WorkerLeaseService
import org.gradle.process.JavaForkOptions
import org.gradle.process.internal.worker.{WorkerProcessBuilder, WorkerProcessFactory}
import org.podval.tools.build.SourceMapper

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

  override protected def createTestDefinitionProcessor(
    workerLeaseService: WorkerLeaseService,
    workerProcessFactory: WorkerProcessFactory,
    workerTestDefinitionProcessorFactory: WorkerTestDefinitionProcessorFactory[TestDefinition],
    javaForkOptions: JavaForkOptions,
    classpath: ForkedTestClasspath,
    workerConfigurationAction: Action[WorkerProcessBuilder]
  ): TestDefinitionProcessor[TestDefinition] = 
    if testsCanNotBeForked then
      workerTestDefinitionProcessorFactory.asInstanceOf[RunTestDefinitionProcessorFactory[TestDefinition]].createNonForking(clock)
    else
      // WriteTestDefinitionProcessor is at the end of the TestDefinitionProcessor chain created in DefaultTestExecuter,
      // right before the ForkingTestDefinitionProcessor,
      // so that PatternMatchTestDefinitionProcessor and RunPreviousFailedFirstTestDefinitionProcessor can do their jobs.
      WriteTestDefinitionProcessor(
        super.createTestDefinitionProcessor(
          workerLeaseService,
          workerProcessFactory,
          workerTestDefinitionProcessorFactory,
          javaForkOptions,
          classpath,
          workerConfigurationAction
        )
      )
