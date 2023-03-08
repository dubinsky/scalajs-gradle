package org.podval.tools.testing.task

import org.gradle.api.file.FileTree
import org.gradle.api.internal.DocumentationRegistry
import org.gradle.api.internal.classpath.ModuleRegistry
import org.gradle.api.internal.tasks.testing.{JvmTestExecutionSpec, TestClassProcessor, TestExecuter, TestFramework,
  TestResultProcessor, WorkerTestClassProcessorFactory}
import org.gradle.api.internal.tasks.testing.detection.{DefaultTestClassScanner, ForkedTestClasspathFactory,
  TestFrameworkDetector}
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.internal.tasks.testing.processors.{MaxNParallelTestClassProcessor, PatternMatchTestClassProcessor,
  RestartEveryNTestClassProcessor, RunPreviousFailedFirstTestClassProcessor, TestMainAction}
import org.gradle.api.internal.tasks.testing.worker.{ForkedTestClasspath, ForkingTestClassProcessor}
import org.gradle.api.logging.{Logger, Logging}
import org.gradle.api.Action
import org.gradle.internal.Factory
import org.gradle.internal.actor.ActorFactory
import org.gradle.internal.time.Clock
import org.gradle.internal.work.WorkerLeaseService
import org.gradle.process.internal.worker.{WorkerProcessBuilder, WorkerProcessFactory}
import org.gradle.process.JavaForkOptions
import java.io.File
import java.util.ArrayList
import scala.jdk.CollectionConverters.*

// Note: translated and improved org.gradle.api.internal.tasks.testing.detection.DefaultTestExecuter
// Note: this is the only Gradle class that I need to modify
// to incorporate NonForkingTestClassProcessor needed for ScalaJS tests.
class DefaultTestExecuter(
  workerFactory: WorkerProcessFactory,
  actorFactory: ActorFactory,
  moduleRegistry: ModuleRegistry,
  workerLeaseService: WorkerLeaseService,
  maxWorkerCount: Int,
  clock: Clock,
  documentationRegistry: DocumentationRegistry,
  testFilter: DefaultTestFilter
) extends TestExecuter[JvmTestExecutionSpec]:
  private val logger: Logger = Logging.getLogger(classOf[DefaultTestExecuter])

  private val testClasspathFactory: ForkedTestClasspathFactory = ForkedTestClasspathFactory(moduleRegistry)

  private var testClassProcessor: Option[TestClassProcessor] = None
  override def stopNow(): Unit = testClassProcessor.foreach(_.stopNow())

  override def execute(testExecutionSpec: JvmTestExecutionSpec, testResultProcessor: TestResultProcessor): Unit = {
    val testFramework: TestFramework = testExecutionSpec.getTestFramework
    val testInstanceFactory: WorkerTestClassProcessorFactory = testFramework.getProcessorFactory

    val classpath: ForkedTestClasspath = testClasspathFactory.create(
      testExecutionSpec.getClasspath,
      testExecutionSpec.getModulePath,
      testFramework,
      testExecutionSpec.getTestIsModule
    )

    val forkingProcessorFactory: Factory[TestClassProcessor] = new Factory[TestClassProcessor]:
      override def create: TestClassProcessor =
        createTestClassProcessor(
          workerLeaseService,
          workerFactory,
          testInstanceFactory,
          testExecutionSpec.getJavaForkOptions,
          classpath,
          testFramework.getWorkerConfigurationAction,
          documentationRegistry
        )

    val reforkingProcessorFactory: Factory[TestClassProcessor] = new Factory[TestClassProcessor]:
      override def create: TestClassProcessor = RestartEveryNTestClassProcessor(
        forkingProcessorFactory,
        testExecutionSpec.getForkEvery
      )

    val processor: TestClassProcessor =
      PatternMatchTestClassProcessor(testFilter,
        RunPreviousFailedFirstTestClassProcessor(testExecutionSpec.getPreviousFailedTestClasses,
          MaxNParallelTestClassProcessor(getMaxParallelForks(testExecutionSpec), reforkingProcessorFactory, actorFactory)))

    testClassProcessor = Some(processor)

    val testClassFiles: FileTree = testExecutionSpec.getCandidateClassFiles

    val testFrameworkDetector: Option[TestFrameworkDetector] =
      if !testExecutionSpec.isScanForTestClasses || testFramework.getDetector == null then None else Some {
        val result: TestFrameworkDetector = testFramework.getDetector
        result.setTestClasses(java.util.ArrayList[File](testExecutionSpec.getTestClassesDirs.getFiles))
        // TODO [classpath] switch to classpath.getApplicationClasspath() without reflection - when it actually starts working ;)
        val applicationClassPath = classpath.getClass.getMethod("getApplicationClasspath").invoke(classpath).asInstanceOf[java.util.List[File]]
        result.setTestClasspath(applicationClassPath)
        result
      }

    TestMainAction(
      DefaultTestClassScanner(testClassFiles, testFrameworkDetector.orNull, processor),
      processor,
      testResultProcessor,
      workerLeaseService,
      clock,
      testExecutionSpec.getPath,
      "Gradle Test Run " + testExecutionSpec.getIdentityPath
    ).run()
  }

  protected def createTestClassProcessor(
    workerLeaseService: WorkerLeaseService,
    workerProcessFactory: WorkerProcessFactory,
    workerTestClassProcessorFactory: WorkerTestClassProcessorFactory,
    javaForkOptions: JavaForkOptions,
    classpath: ForkedTestClasspath,
    workerConfigurationAction: Action[WorkerProcessBuilder],
    documentationRegistry: DocumentationRegistry
  ): TestClassProcessor = ForkingTestClassProcessor(
    workerLeaseService,
    workerProcessFactory,
    workerTestClassProcessorFactory,
    javaForkOptions,
    classpath,
    workerConfigurationAction,
    documentationRegistry
  )

  private def getMaxParallelForks(testExecutionSpec: JvmTestExecutionSpec): Int =
    var maxParallelForks: Int = testExecutionSpec.getMaxParallelForks
    if maxParallelForks > maxWorkerCount then
      logger.info("{}.maxParallelForks ({}) is larger than max-workers ({}), forcing it to {}", testExecutionSpec.getPath, maxParallelForks, maxWorkerCount, maxWorkerCount)
      maxParallelForks = maxWorkerCount

    maxParallelForks
