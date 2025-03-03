package org.podval.tools.test.task

import org.gradle.api.Action
import org.gradle.api.internal.DocumentationRegistry
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
import org.podval.tools.test.SourceMapper
import org.podval.tools.test.processor.{RunTestClassProcessorFactory, WriteTestClassProcessor}
import org.podval.tools.test.result.{RootTestSuiteOutputFixingTestResultProcessor, SourceMappingTestResultProcessor,
  TracingTestResultProcessor}

class TestExecuter(
  canFork: Boolean,
  sourceMapper: Option[SourceMapper],
  testFilter: DefaultTestFilter,
  maxWorkerCount: Int,
  clock: Clock,
  workerProcessFactory: WorkerProcessFactory,
  actorFactory: ActorFactory,
  workerLeaseService: WorkerLeaseService,
  moduleRegistry: ModuleRegistry,
  documentationRegistry: DocumentationRegistry
) extends DefaultTestExecuter(
  workerProcessFactory,
  actorFactory,
  moduleRegistry,
  workerLeaseService,
  maxWorkerCount,
  clock,
  documentationRegistry,
  testFilter
):
  override def execute(
    testExecutionSpec: JvmTestExecutionSpec,
    testResultProcessor: TestResultProcessor
  ): Unit =
    // Note: deeper down, TestMainAction wraps testResultProcessorEffective in AttachParentTestResultProcessor.
    val testResultProcessorEffective: TestResultProcessor =
      RootTestSuiteOutputFixingTestResultProcessor(
        SourceMappingTestResultProcessor(
          TracingTestResultProcessor(
            testResultProcessor,
            clock,
            isEnabled = false
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
    workerConfigurationAction: Action[WorkerProcessBuilder],
    documentationRegistry: DocumentationRegistry
  ): TestClassProcessor =
    // Scala.js tests must be run in the same JVM where their frameworks were instantiated.
    val doNotFork: Boolean = !canFork
    if doNotFork then
      workerTestClassProcessorFactory.asInstanceOf[RunTestClassProcessorFactory].createNonForking(clock)
    else
      // Encoding of TestClassRun happens at the end of the TestClassProcessor chain,
      // so that PatternMatchTestClassProcessor and RunPreviousFailedFirstTestClassProcessor
      // can do their jobs.
      WriteTestClassProcessor(
        super.createTestClassProcessor(
          workerLeaseService,
          workerProcessFactory,
          workerTestClassProcessorFactory,
          javaForkOptions,
          classpath,
          workerConfigurationAction,
          documentationRegistry
        )
      )
