package org.podval.tools.test

import org.gradle.api.Action
import org.gradle.api.internal.tasks.testing.{JvmTestExecutionSpec, TestResultProcessor, TestClassProcessor as GTestClassProcessor}
import org.gradle.api.internal.tasks.testing.processors.{MaxNParallelTestClassProcessor, RestartEveryNTestClassProcessor,
  RunPreviousFailedFirstTestClassProcessor}
import org.gradle.api.internal.tasks.testing.worker.ForkingTestClassProcessor
import org.gradle.api.internal.DocumentationRegistry
import org.gradle.api.logging.Logging
import org.gradle.internal.Factory
import org.gradle.internal.work.WorkerLeaseService
import org.gradle.process.internal.worker.{WorkerProcessBuilder, WorkerProcessFactory}
import org.gradle.api.internal.classpath.ModuleRegistry
import org.gradle.internal.actor.{Actor, ActorFactory}
import org.gradle.internal.service.ServiceRegistry
import org.gradle.internal.time.Clock
import org.opentorah.build.Gradle
import org.opentorah.util.Files
import sbt.testing.Framework
import java.io.File
import scala.jdk.CollectionConverters.*

// Note: I am following the lead of the org.gradle.api.tasks.testing.Test
// and separating the TestExecuter from the TestTask -
// maybe it will help with the classpath disaster...
// Note: I probably do not need to be too careful with using only Java types when creating ForkingTestClassProcessor -
// this stuff does not get serialized...
final class TestExecuter(
  testClassPath: Array[File],
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
  includes: Set[String],
  excludes: Set[String],
  commandLineIncludes: Set[String],
  includeTags: Array[String],
  excludeTags: Array[String],
  serviceRegistry: ServiceRegistry // TODO remove when parallelization works
) extends org.gradle.api.internal.tasks.testing.TestExecuter[JvmTestExecutionSpec]:

  private var testClassProcessorOpt: Option[GTestClassProcessor] = None

  override def stopNow(): Unit = testClassProcessorOpt.foreach(_.stopNow())

  override def execute(
    testExecutionSpec: JvmTestExecutionSpec,
    gTestResultProcessor: TestResultProcessor
  ): Unit =

    val testClassProcessor: GTestClassProcessor = createTestClassProcessor(
      testExecutionSpec = testExecutionSpec,
      testClassPath = testClassPath
    )

    testClassProcessorOpt = Some(testClassProcessor)

    val loadedFrameworks: List[Framework] = testEnvironment.loadAllFrameworks

    val testClassScanner: TestClassScanner = TestClassScanner(
      loadedFrameworks = loadedFrameworks,
      analysisFile = scalaCompileAnalysisFile,
      includes = includes,
      excludes = excludes,
      commandLineIncludes = commandLineIncludes,
      testClassProcessor = testClassProcessor
    )

    val testResultProcessor: TestResultProcessor =
      SourceMappingTestResultProcessor(
        gTestResultProcessor,
        sourceMapper
      )

    // TODO remove when everything is parallelized: MaxNParallelTestClassProcessor wraps resultProcessor in an Actor already...
    val (resultProcessorActor: Option[Actor], resultProcessor: TestResultProcessor) =
      if !TestExecuter.doNotFork then (None, testResultProcessor) else
        val actor: Actor = actorFactory.createActor(testResultProcessor)
        (Some(actor), actor.getProxy(classOf[TestResultProcessor]))

    TestRoot(
      testEnvironment = testEnvironment,
      loadedFrameworks = loadedFrameworks,
      testClassScanner = testClassScanner,
      testClassProcessor = testClassProcessor,
      testResultProcessor = resultProcessor,
      clock = clock,
      sbtClassPath = sbtClassPath,
      workerLeaseService = workerLeaseService
    ).run()

    // TODO MaxNParallelTestClassProcessor uses CompositeStoppable
    resultProcessorActor.foreach(_.stop())

  private def createTestClassProcessor(
    testExecutionSpec: JvmTestExecutionSpec,
    testClassPath: Array[File]
  ): GTestClassProcessor =
    val workerTestClassProcessorFactory: WorkerTestClassProcessorFactory = WorkerTestClassProcessorFactory(
      testClassPath = testClassPath,
      runningInIntelliJIdea = runningInIntelliJIdea,
      includeTags = includeTags,
      excludeTags = excludeTags
    )

    if TestExecuter.doNotFork then workerTestClassProcessorFactory.create(serviceRegistry) else
      val forkingProcessorFactory: Factory[GTestClassProcessor] = () => ForkingTestClassProcessor(
        workerLeaseService,
        workerProcessFactory,
        workerTestClassProcessorFactory,
        testExecutionSpec.getJavaForkOptions,
        TestExecuter.immutableCopy(testExecutionSpec.getClasspath),
        TestExecuter.immutableCopy(testExecutionSpec.getModulePath),
        testWorkerImplementationModules,
        workerConfigurationAction,
        moduleRegistry,
        documentationRegistry
      )

      val reforkingProcessorFactory: Factory[GTestClassProcessor] = () => RestartEveryNTestClassProcessor(
        forkingProcessorFactory,
        testExecutionSpec.getForkEvery
      )

  //    new PatternMatchTestClassProcessor(
  //      testFilter,
  //      new RunPreviousFailedFirstTestClassProcessor(
  //        testExecutionSpec.getPreviousFailedTestClasses,
      MaxNParallelTestClassProcessor(
        getMaxParallelForks(testExecutionSpec),
        reforkingProcessorFactory,
        actorFactory
      )
  //      )
  //    )

  private def getMaxParallelForks(testExecutionSpec: JvmTestExecutionSpec): Int =
    val maxParallelForks: Int = testExecutionSpec.getMaxParallelForks
    if maxParallelForks <= maxWorkerCount then maxParallelForks else
      Logging.getLogger(classOf[TestExecuter])
        .info(s"${testExecutionSpec.getPath}.maxParallelForks ($maxParallelForks) is larger than max-workers ($maxWorkerCount), forcing it to $maxWorkerCount",
        null, null, null
        )
      maxWorkerCount

  // Note: here I make sure that my classes are on the worker's classpath.
  // Note: I am not clear on what the ForkingTestClassProcessor does with its classPath parameter...
  // Note: it would be nice to add what I need as modules, but as far as I can tell,
  // those are only looked up in some Gradle module registry...
  private val testWorkerImplementationModules: java.util.List[String] = java.util.Collections.emptyList[String]
  private val workerConfigurationAction: Action[WorkerProcessBuilder] = (builder: WorkerProcessBuilder) =>
    // If I do nothing, I get ClassNotFoundException for org.podval.tools.test.TestClassProcessorFactory.
    // Apparently, I need to add the plugin classes to some classpath...
    // When I do this:

//    val applicationClasspath: List[String] = List(
//      "org.podval.tools.scalajs"
//    )
//    builder.applicationClasspath(applicationClasspath.map(TestExecuter.artifactUrl).map(Files.url2file).asJava)
//    builder.sharedPackages(
//      "org.podval.tools.test",
////      "org.gradle.api.internal.tasks.testing"
//    )

    // I no longer get ClassNotFoundException for org.podval.tools.test.TestClassProcessorFactory,
    // but I do get it for the base class org.gradle.api.internal.tasks.testing.WorkerTestClassProcessorFactory -
    // even though ForkingTestClassProcessor.getTestWorkerImplementationClasspath()
    // includes the module where that class resides ("gradle-testing-base")
    // so setImplementationClasspath() should include it.
    //
    // Documentation for WorkerProcessSettings (from which WorkerProcessBuilder is derived) says:
    //   A worker process can optionally specify an application classpath.
    //   The classes of this classpath are loaded into an isolated ClassLoader,
    //   which is made visible to the worker action ClassLoader.
    //   Only the packages specified in the set of shared packages are visible to the worker action ClassLoader.
    //
    // It seems that the classes I add via applicationClasspath() are visible to the implementation
    // (and the action if I make them shared), but the implementation classes are not visible to the action?
    //
    // Since:
    // - I can't find the modules to use testWorkerImplementationModules;
    // - I can't make applicationClasspath() work;
    // - setImplementationClasspath() is used by the ForkingTestClassProcessor,
    //   and since getTestWorkerImplementationClasspath() is not public and
    //   there is no WorkerProcessBuilder.getImplementationClasspath(), I can't augment it;
    //
    // I resort to using setImplementationModulePath().
    // By now ForkingTestClassProcessor.forkProcess() already called it with an empty list of Urls
    // resulting from the empty list of module names that I give to it in testWorkerImplementationModules.
    // So whatever I set via an setImplementationModulePath() call here wins :)

    // test-interface
    val implementationClassPath: List[String] = List(
      "org.podval.tools.scalajs",
      "scala3-library",
      "scala-library"
    )
    builder.setImplementationModulePath(
      implementationClassPath.map((name: String) => Gradle.findOnClasPath(TestExecuter, name)).asJava
    )
//    builder.setUseLegacyAddOpens(true)

object TestExecuter:
  // TODO make this an option on the TestTask class - unless one is already there ;)
  val doNotFork: Boolean = true

  private def immutableCopy(iterable: java.lang.Iterable[? <: File]): java.lang.Iterable[File] = Set.from(iterable.asScala).asJava
