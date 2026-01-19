package org.podval.tools.test.task

import org.gradle.api.file.FileTree
import org.gradle.api.internal.classpath.ModuleRegistry
import org.gradle.api.internal.file.FileCollectionFactory
import org.gradle.api.internal.tasks.testing.{JvmTestExecutionSpec, TestDefinition, TestDefinitionProcessor,
  TestExecuter, TestFramework, TestResultProcessor, WorkerTestDefinitionProcessorFactory}
import org.gradle.api.internal.tasks.testing.detection.{DefaultTestScanner, ForkedTestClasspathFactory, TestDetector,
  TestFrameworkDetector}
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.internal.tasks.testing.processors.{MaxNParallelTestDefinitionProcessor,
  PatternMatchTestDefinitionProcessor, RestartEveryNTestDefinitionProcessor, TestMainAction}
import org.gradle.api.internal.tasks.testing.results.TestRetryShieldingTestResultProcessor
import org.gradle.api.internal.tasks.testing.worker.{ForkedTestClasspath, ForkingTestDefinitionProcessor}
import org.gradle.api.logging.{Logger, Logging}
import org.gradle.api.Action
import org.gradle.internal.{Cast, Factory}
import org.gradle.internal.actor.ActorFactory
import org.gradle.internal.time.Clock
import org.gradle.internal.work.WorkerLeaseService
import org.gradle.process.internal.worker.{WorkerProcessBuilder, WorkerProcessFactory}
import org.gradle.process.JavaForkOptions
import org.podval.tools.platform.Reflection
import org.podval.tools.test.run.RunPreviousFailedFirstTestDefinitionProcessor
import java.io.File

// Translated and improved org.gradle.api.internal.tasks.testing.detection.DefaultTestExecuter.
// This is the only Gradle class that I need to fork, modify and maintain -
// to use NonForkingTestDefinitionProcessor needed for Scala.js and Scala Native tests.
// Update: starting with Gradle 9.3.0, I also need to maintain a fork of
// org.gradle.api.internal.tasks.testing.processors.RunPreviousFailedFirstTestDefinitionProcessor :(
open class DefaultTestExecuter(
  workerFactory: WorkerProcessFactory,
  actorFactory: ActorFactory,
  moduleRegistry: ModuleRegistry,
  workerLeaseService: WorkerLeaseService,
  maxWorkerCount: Int,
  clock: Clock,
  testFilter: DefaultTestFilter
) extends TestExecuter[JvmTestExecutionSpec]:
  private val logger: Logger = Logging.getLogger(getClass)

  private val testClasspathFactory: ForkedTestClasspathFactory = ForkedTestClasspathFactory(moduleRegistry)

  private var testDefinitionProcessor: Option[TestDefinitionProcessor[TestDefinition]] = None
  override def stopNow(): Unit = testDefinitionProcessor.foreach(_.stopNow())

  override def execute(testExecutionSpec: JvmTestExecutionSpec, testResultProcessor: TestResultProcessor): Unit =
    val testFramework: TestFramework = testExecutionSpec.getTestFramework

    // Cast away from ? so we don't need to propagate the wildcard everywhere
    // This is safe because the frameworks that don't accept all TestDefinitions will have the dir selection filtered out earlier
    // If a TestFramework begins to reject ClassTestDefinitions, this needs rethinking.
    val testInstanceFactory: WorkerTestDefinitionProcessorFactory[TestDefinition] = Cast.uncheckedNonnullCast(
      testFramework.getProcessorFactory
    )

    val classpath: ForkedTestClasspath = testClasspathFactory.create(
      testExecutionSpec.getClasspath,
      testExecutionSpec.getModulePath
    )

    val forkingProcessorFactory: Factory[TestDefinitionProcessor[TestDefinition]] = new Factory[TestDefinitionProcessor[TestDefinition]]:
      override def create: TestDefinitionProcessor[TestDefinition] =
        createTestDefinitionProcessor(
          workerLeaseService,
          workerFactory,
          testInstanceFactory,
          testExecutionSpec.getJavaForkOptions,
          classpath,
          testFramework.getWorkerConfigurationAction
        )

    val reforkingProcessorFactory: Factory[TestDefinitionProcessor[TestDefinition]] = new Factory[TestDefinitionProcessor[TestDefinition]]:
      override def create: TestDefinitionProcessor[TestDefinition] = RestartEveryNTestDefinitionProcessor(
        forkingProcessorFactory,
        testExecutionSpec.getForkEvery
      )

    val processor: TestDefinitionProcessor[TestDefinition] =
      PatternMatchTestDefinitionProcessor(testFilter,
        RunPreviousFailedFirstTestDefinitionProcessor(testExecutionSpec.getPreviousFailedTestClasses, java.util.Collections.emptySet(),
          MaxNParallelTestDefinitionProcessor(getMaxParallelForks(testExecutionSpec), reforkingProcessorFactory, actorFactory)))

    testDefinitionProcessor = Some(processor)

    val testClassFiles: FileTree =
      if testExecutionSpec.isScanForTestClasses
      then testExecutionSpec.getCandidateClassFiles
      else FileCollectionFactory.emptyTree
      testExecutionSpec.getCandidateClassFiles

    val testDefinitionDirs: java.util.Set[File] = testExecutionSpec.getCandidateTestDefinitionDirs

    val testFrameworkDetector: Option[TestFrameworkDetector] =
      if testFramework.getDetector == null then None else Some:
        val result: TestFrameworkDetector = testFramework.getDetector
        result.setTestClasses(java.util.ArrayList[File](testExecutionSpec.getTestClassesDirs.getFiles))
        result.setTestClasspath(
          Reflection.Invoke[java.util.List[File], ForkedTestClasspath]("getApplicationClasspath")(classpath)
        )
        result

    val detector: TestDetector = new DefaultTestScanner(testClassFiles, testDefinitionDirs, testFrameworkDetector.orNull, processor)

    // What is this?
    // In some versions of the Gradle retry plugin, it would retry any test that had any kind of failure associated with it.
    // We attempt to capture assumption violations as failures for skipped tests.
    //
    // This would cause any test that had been skipped to be executed multiple times. This could sometimes cause real failures.
    // To work around this, we shield the test retry result processor from seeing test assumption failures.
    val testResultProcessorEffective: TestResultProcessor = if testResultProcessor == null then null else
      // KMP calls this code with a delegating test result processor that does not return sensible Class objects
      val canonicalName: String = testResultProcessor.getClass.getCanonicalName
      if canonicalName != null && canonicalName.endsWith("org.gradle.testretry.internal.executer.RetryTestResultProcessor")
      then TestRetryShieldingTestResultProcessor(testResultProcessor)
      else testResultProcessor

    TestMainAction(
      detector,
      processor,
      testResultProcessorEffective,
      workerLeaseService,
      clock,
      testExecutionSpec.getPath,
      "Gradle Test Run " + testExecutionSpec.getIdentityPath
    ).run()

  protected def createTestDefinitionProcessor(
    workerLeaseService: WorkerLeaseService,
    workerProcessFactory: WorkerProcessFactory,
    workerTestDefinitionProcessorFactory: WorkerTestDefinitionProcessorFactory[TestDefinition],
    javaForkOptions: JavaForkOptions,
    classpath: ForkedTestClasspath,
    workerConfigurationAction: Action[WorkerProcessBuilder]
  ): TestDefinitionProcessor[TestDefinition] = ForkingTestDefinitionProcessor[TestDefinition](
    workerLeaseService,
    workerProcessFactory,
    workerTestDefinitionProcessorFactory,
    javaForkOptions,
    classpath,
    workerConfigurationAction
  )

  private def getMaxParallelForks(testExecutionSpec: JvmTestExecutionSpec): Int =
    var maxParallelForks: Int = testExecutionSpec.getMaxParallelForks
    if maxParallelForks > maxWorkerCount then
      logger.info(
        "{}.maxParallelForks ({}) is larger than max-workers ({}), forcing it to {}",
        testExecutionSpec.getPath,
        maxParallelForks,
        maxWorkerCount, 
        maxWorkerCount
      )
      maxParallelForks = maxWorkerCount

    maxParallelForks
