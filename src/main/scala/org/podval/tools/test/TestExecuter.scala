package org.podval.tools.test

import org.gradle.api.Action
import org.gradle.api.internal.tasks.testing.{JvmTestExecutionSpec, TestClassProcessor as GTestClassProcessor, TestResultProcessor}
import org.gradle.api.internal.tasks.testing.processors.{MaxNParallelTestClassProcessor, RestartEveryNTestClassProcessor,
  RunPreviousFailedFirstTestClassProcessor}
import org.gradle.api.internal.DocumentationRegistry
import org.gradle.api.internal.classpath.ModuleRegistry
import org.gradle.api.logging.Logging
import org.gradle.internal.Factory
import org.gradle.internal.actor.{Actor, ActorFactory}
import org.gradle.internal.service.{DefaultServiceRegistry, ServiceRegistry}
import org.gradle.internal.time.Clock
import org.gradle.internal.work.WorkerLeaseService
import org.gradle.process.internal.worker.{WorkerProcessBuilder, WorkerProcessFactory}
import org.gradle.process.JavaForkOptions
import org.opentorah.build.Gradle
import org.opentorah.util.Files
import org.podval.tools.test.framework.FrameworkDescriptor
import sbt.testing.Framework
import java.io.File
import java.net.URL
import scala.jdk.CollectionConverters.*
import TestResultProcessorEx.*

// Note: I am following the lead of the org.gradle.api.tasks.testing.Test and separating the TestExecuter from the TestTask.
final class TestExecuter(
  groupByFramework: Boolean,
  canFork: Boolean,
  sourceMapper: Option[SourceMapper],
  testEnvironment: TestEnvironment,
  scalaCompileAnalysisFile: File,
  runningInIntelliJIdea: Boolean,
  sbtClassPath: Iterable[File],
  workerProcessFactory: WorkerProcessFactory,
  actorFactory: ActorFactory,
  moduleRegistry: ModuleRegistry,
  workerLeaseService: WorkerLeaseService,
  maxWorkerCount: Int,
  clock: Clock,
  documentationRegistry: DocumentationRegistry,
  testFiltering: TestFiltering,
  testTagging: TestTagging
) extends org.gradle.api.internal.tasks.testing.TestExecuter[JvmTestExecutionSpec]:

  private var testClassProcessorOpt: Option[GTestClassProcessor] = None

  override def stopNow(): Unit = testClassProcessorOpt.foreach(_.stopNow())

  override def execute(
    testExecutionSpec: JvmTestExecutionSpec,
    gTestResultProcessor: TestResultProcessor
  ): Unit =

    val maxParallelForks: Int =
      val maxParallelForks: Int = testExecutionSpec.getMaxParallelForks
      if maxParallelForks <= maxWorkerCount then maxParallelForks else
        Logging.getLogger(classOf[TestExecuter])
          .info(s"${testExecutionSpec.getPath}.maxParallelForks ($maxParallelForks) is larger than max-workers ($maxWorkerCount), forcing it to $maxWorkerCount",
            null, null, null
          )
        maxWorkerCount

    val shouldFork: Boolean = maxParallelForks > 1

    if shouldFork && !canFork then Logging.getLogger(classOf[TestExecuter])
      .info(s"Can not fork tests; maxParallelForks setting ($maxParallelForks) ignored", null, null, null)

    val isForked: Boolean = canFork & shouldFork

    val testClassPath: Iterable[File] = testExecutionSpec.getClasspath.asScala

    val testClassProcessor: GTestClassProcessor =
      if !isForked then TestClassProcessor(
        groupByFramework = groupByFramework,
        isForked = false,
        testClassPath = testClassPath.toArray,
        runningInIntelliJIdea = runningInIntelliJIdea,
        testTagging = testTagging,
        clock = clock
      ) else createForkingTestClassProcessor(
        testClassPath = testClassPath,
        applicationModulePath = testExecutionSpec.getModulePath.asScala, // Note: empty
        previousFailedTestClasses = testExecutionSpec.getPreviousFailedTestClasses,
        maxParallelForks = maxParallelForks,
        forkEvery = testExecutionSpec.getForkEvery,
        forkOptions = testExecutionSpec.getJavaForkOptions
      )

    val testResultProcessor: TestResultProcessor = SourceMappingTestResultProcessor(
      gTestResultProcessor,
      sourceMapper
    )

    testClassProcessorOpt = Some(testClassProcessor)

    val resultProcessorActor: Option[Actor] =
      if isForked
      then None // MaxNParallelTestClassProcessor used by the createForkingTestClassProcessor wraps resultProcessor in an Actor already.
      else Some(actorFactory.createActor(testResultProcessor))

    run(
      loadedFrameworks = testEnvironment.loadAllFrameworks,
      testClassProcessor = testClassProcessor,
      testResultProcessor = resultProcessorActor.fold(testResultProcessor)(_.getProxy(classOf[TestResultProcessor]))
    )

    resultProcessorActor.foreach(_.stop())

  private def createForkingTestClassProcessor(
    testClassPath: Iterable[File],
    applicationModulePath: Iterable[File],
    previousFailedTestClasses: java.util.Set[String],
    maxParallelForks: Int,
    forkEvery: Long,
    forkOptions: JavaForkOptions
  ): GTestClassProcessor =
    val workerTestClassProcessorFactory: WorkerTestClassProcessorFactory = WorkerTestClassProcessorFactory(
      groupByFramework = groupByFramework,
      testClassPath = testClassPath.toArray,
      runningInIntelliJIdea = runningInIntelliJIdea,
      testTagging = testTagging
    )

    // Note: here I make sure that my classes are on the worker's classpath(s);
    // it would be nice to add what I need as modules, but as far as I can tell,
    // those are only looked up in some Gradle module registry.
    // testExecutionSpec.getClasspath contains the testing frameworks.

    // The only thing remaining is to figure out to which classpath to add what I need
    // (application or implementation) and how to share it so that I stop getting
    // ClassNotFound but do not start getting CanNotCast ;)

    // nothing added: CNF org.podval.tools.test.TestWorker
    //   add org.podval.tools.scalajs to the applicationClassPath: same
    //     add org.podval.tools.test to sharedPackages: CNF org.gradle.api.Action
    //       it seems that Gradle classes from the implementation classpath
    //       are not available on the application classpath
    //       XXX
    //   add org.podval.tools.scalajs to the implementationClassPath: CNF scala.CanEqual
    //     add scala3-library and scala-library to the implementationClassPath: CNF sbt.testing.Fingerprint
    //       add test-interface to the applicationClassPath and sbt.testing to the sharedPackages: CNF org.scalatest.tools.Framework
    //         add org.scalatest.tools to the sharedPackages

    val implementationClassPath: List[URL] = List(
      "org.podval.tools.scalajs",
      "scala3-library",
      "scala-library"
    ).map(TestExecuter.findOnClassPath)

    val applicationClassPath: List[File] = testClassPath.toList ++ List(
      "test-interface"
    ).map(TestExecuter.findOnClassPath).map(Files.url2file)

    val sharedPackages: List[String] = List(
      "sbt.testing"
    ) ++ FrameworkDescriptor.all.flatMap(_.sharedPackages)

    val forkingProcessorFactory: Factory[GTestClassProcessor] = () => ForkingTestClassProcessor(
      workerThreadRegistry = workerLeaseService,
      workerFactory = workerProcessFactory,
      processorFactory = workerTestClassProcessorFactory,
      options = forkOptions,
      applicationClassPath = applicationClassPath,
      applicationModulePath = applicationModulePath,
      implementationClassPath = implementationClassPath,
      implementationModules = List.empty,
      sharedPackages = sharedPackages,
      moduleRegistry = moduleRegistry,
      documentationRegistry = documentationRegistry
    )

    val reforkingProcessorFactory: Factory[GTestClassProcessor] = () => RestartEveryNTestClassProcessor(
      forkingProcessorFactory,
      forkEvery
    )

    // Note: not wrapping in new PatternMatchTestClassProcessor(testFilter, _) since I do my own filtering.
    RunPreviousFailedFirstTestClassProcessor(
      previousFailedTestClasses,
      MaxNParallelTestClassProcessor(
        maxParallelForks,
        reforkingProcessorFactory,
        actorFactory
      )
    )

  private def run(
    loadedFrameworks: List[Framework],
    testClassProcessor: GTestClassProcessor,
    testResultProcessor: TestResultProcessor
  ): Unit =
    val startTime: Long = clock.getCurrentTime
    testResultProcessor.started(RootTest, startTime)
    val frameworkTests: List[FrameworkTest] = loadedFrameworks.map(RootTest.forFramework)
    if groupByFramework then frameworkTests.foreach(testResultProcessor.started(_, startTime))

    try
      testClassProcessor.startProcessing(testResultProcessor)
      try
        Gradle.addToClassPath(this, sbtClassPath)
        TestClassScanner.run(
          groupByFramework = groupByFramework,
          loadedFrameworks = loadedFrameworks,
          analysisFile = scalaCompileAnalysisFile,
          testClassProcessor = testClassProcessor,
          testFiltering = testFiltering
        )
      finally
      // Release worker lease while waiting for tests to complete
        workerLeaseService.blocking(() => testClassProcessor.stop())
    finally
      val endTime: Long = clock.getCurrentTime
      if groupByFramework then frameworkTests.foreach(testResultProcessor.completed(_, endTime))
      testResultProcessor.completed(RootTest, endTime)
      testEnvironment.close()

object TestExecuter:
  private def findOnClassPath(name: String): URL = Gradle.findOnClasPath(TestExecuter, name)
