package org.podval.tools.test

import org.gradle.StartParameter
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.DocumentationRegistry
import org.gradle.api.internal.classpath.ModuleRegistry
import org.gradle.api.internal.tasks.testing.{JvmTestExecutionSpec, TestExecuter, TestResultProcessor,
  TestClassProcessor as GTestClassProcessor}
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.internal.tasks.testing.processors.{MaxNParallelTestClassProcessor, RestartEveryNTestClassProcessor,
  RunPreviousFailedFirstTestClassProcessor}
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.provider.{Property, SetProperty}
import org.gradle.api.tasks.{Classpath, Input, Optional, SourceSet}
import org.gradle.api.tasks.testing.{AbstractTestTask, Test, TestListener}
import org.gradle.internal.Factory
import org.gradle.internal.actor.{Actor, ActorFactory}
import org.gradle.internal.event.ListenerBroadcast
import org.gradle.internal.time.Clock
import org.gradle.internal.work.WorkerLeaseService
import org.gradle.process.JavaForkOptions
import org.gradle.process.internal.worker.WorkerProcessFactory
import org.opentorah.build.Gradle
import org.opentorah.build.Gradle.*
import org.opentorah.util.Files
import org.podval.tools.test.framework.FrameworkDescriptor
import java.io.File
import java.lang.reflect.Field
import java.net.URL
import scala.jdk.CollectionConverters.*
import sbt.testing.Framework
import TestResultProcessorEx.*

// guide: https://docs.gradle.org/current/userguide/java_testing.html
// configuration: https://docs.gradle.org/current/dsl/org.gradle.api.tasks.testing.Test.html
abstract class TestTask extends Test:
  setGroup(JavaBasePlugin.VERIFICATION_GROUP)

  private def sourceSet: SourceSet = getProject.getSourceSet(SourceSet.TEST_SOURCE_SET_NAME)

  @Classpath final def getRuntimeClassPath: FileCollection = sourceSet.getRuntimeClasspath
  getProject.afterEvaluate((project: Project) =>
    getDependsOn.add(project.getClassesTask(sourceSet))
    ()
  )

  @Input @Optional def getGroupByFramework: Property[Boolean]
  @Input def getIncludeTags: SetProperty[String]
  @Input def getExcludeTags: SetProperty[String]

  protected def canFork: Boolean
  protected def sourceMapper: Option[SourceMapper]
  protected def testEnvironment: TestEnvironment

  final override def createTestExecuter: TestExecuter[JvmTestExecutionSpec] = new TestExecuter[JvmTestExecutionSpec]:
    private var testClassProcessorOpt: Option[GTestClassProcessor] = None
    override def stopNow(): Unit = testClassProcessorOpt.foreach(_.stopNow())

    override def execute(
      testExecutionSpec: JvmTestExecutionSpec,
      testResultProcessor: TestResultProcessor
    ): Unit =
      val maxWorkerCount: Int = getServices.get(classOf[StartParameter]).getMaxWorkerCount
      val maxParallelForks: Int =
        val maxParallelForks: Int = testExecutionSpec.getMaxParallelForks
        if maxParallelForks <= maxWorkerCount then maxParallelForks else
          getLogger.info(s"${testExecutionSpec.getPath}.maxParallelForks ($maxParallelForks) is larger than max-workers ($maxWorkerCount), forcing it to $maxWorkerCount", null, null, null)
          maxWorkerCount

      val shouldFork: Boolean = maxParallelForks > 1
      if shouldFork && !canFork then getLogger.info(s"Can not fork tests; maxParallelForks setting ($maxParallelForks) ignored", null, null, null)
      val isForked: Boolean = canFork & shouldFork

      val groupByFramework: Boolean = getGroupByFramework.toOption.getOrElse(false)
      val testClassPath: Array[File] = testExecutionSpec.getClasspath.asScala.toArray

      val workerTestClassProcessorFactory = WorkerTestClassProcessorFactory(
        groupByFramework = groupByFramework,
        runningInIntelliJIdea = TestTask.runningInIntelliJIdea(TestTask.this),
        testClassPath = testClassPath,
        testTagsFilter = TestTagsFilter(
          include = getIncludeTags.get.asScala.toArray,
          exclude = getExcludeTags.get.asScala.toArray
        )
      )

      val testClassProcessor: GTestClassProcessor = if !isForked then workerTestClassProcessorFactory.create(
        clock = getServices.get(classOf[Clock])
      ) else createForkingTestClassProcessor(
        testClassPath = testClassPath,
        maxParallelForks = maxParallelForks,
        forkEvery = testExecutionSpec.getForkEvery,
        forkOptions = testExecutionSpec.getJavaForkOptions,
        applicationModulePath = testExecutionSpec.getModulePath.asScala, // Note: empty
        workerTestClassProcessorFactory = workerTestClassProcessorFactory
      )

      testClassProcessorOpt = Some(RunPreviousFailedFirstTestClassProcessor(
        testExecutionSpec.getPreviousFailedTestClasses,
        testClassProcessor
      ))

      run(
        isForked = isForked,
        groupByFramework = groupByFramework,
        testClassProcessor = testClassProcessorOpt.get,
        testResultProcessorRaw = SourceMappingTestResultProcessor(
          testResultProcessor,
          sourceMapper
        )
      )

  private def createForkingTestClassProcessor(
    workerTestClassProcessorFactory: WorkerTestClassProcessorFactory,
    testClassPath: Array[File],
    applicationModulePath: Iterable[File],
    maxParallelForks: Int,
    forkEvery: Long,
    forkOptions: JavaForkOptions
  ): GTestClassProcessor =

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
    ).map(TestTask.findOnClassPath)

    val applicationClassPath: List[File] = testClassPath.toList ++ List(
      "test-interface"
    ).map(TestTask.findOnClassPath).map(Files.url2file)

    val sharedPackages: List[String] = List(
      "sbt.testing"
    ) ++ FrameworkDescriptor.all.flatMap(_.sharedPackages)

    val forkingProcessorFactory: Factory[GTestClassProcessor] = () => ForkingTestClassProcessor(
      workerThreadRegistry = getServices.get(classOf[WorkerLeaseService]),
      workerFactory = getProcessBuilderFactory,
      processorFactory = workerTestClassProcessorFactory,
      options = forkOptions,
      applicationClassPath = applicationClassPath,
      applicationModulePath = applicationModulePath,
      implementationClassPath = implementationClassPath,
      implementationModules = List.empty,
      sharedPackages = sharedPackages,
      moduleRegistry = getModuleRegistry,
      documentationRegistry = getServices.get(classOf[DocumentationRegistry])
    )

    val reforkingProcessorFactory: Factory[GTestClassProcessor] = () => RestartEveryNTestClassProcessor(
      forkingProcessorFactory,
      forkEvery
    )

    // Note: not wrapping in new PatternMatchTestClassProcessor(testFilter, _) since I do my own filtering.
    MaxNParallelTestClassProcessor(
      maxParallelForks,
      reforkingProcessorFactory,
      getActorFactory
    )

  private def run(
    isForked: Boolean,
    groupByFramework: Boolean,
    testClassProcessor: GTestClassProcessor,
    testResultProcessorRaw: TestResultProcessor
  ): Unit =
    val clock: Clock = getServices.get(classOf[Clock])
    val startTime: Long = clock.getCurrentTime

    val testEnvironment: TestEnvironment = this.testEnvironment
    val loadedFrameworks: List[Framework] = testEnvironment.loadAllFrameworks

    // MaxNParallelTestClassProcessor used when forking wraps resultProcessor in an Actor already.
    val resultProcessorActor: Option[Actor] =
      if isForked then None else Some(getActorFactory.createActor(testResultProcessorRaw))
    val testResultProcessor: TestResultProcessor =
      resultProcessorActor.fold(testResultProcessorRaw)(_.getProxy(classOf[TestResultProcessor]))

    testResultProcessor.started(RootTest, startTime)
    val frameworkTests: List[FrameworkTest] = loadedFrameworks.map(RootTest.forFramework)
    if groupByFramework then frameworkTests.foreach(testResultProcessor.started(_, startTime))

    try
      testClassProcessor.startProcessing(testResultProcessor)
      try
        Gradle.addToClassPath(this, getProject.getConfiguration(Sbt.configurationName).asScala)
        TestScanner.run(
          groupByFramework = groupByFramework,
          loadedFrameworks = loadedFrameworks,
          testClassProcessor = testClassProcessor,
          // Note: scalaCompile.getAnalysisFiles is empty, so I had to hard-code the path:
          analysisFile = Files.file(
            directory = getProject.getBuildDir,
            segments = s"tmp/scala/compilerAnalysis/${getProject.getScalaCompile(sourceSet).getName}.analysis"
          ),
          testFilter = TestFilter(
            includes = getFilter.getIncludePatterns.asScala.toSet,
            excludes = getFilter.getExcludePatterns.asScala.toSet,
            commandLineIncludes = getFilter.asInstanceOf[DefaultTestFilter].getCommandLineIncludePatterns.asScala.toSet
          )
        )
      finally
        // Release worker lease while waiting for tests to complete
        getServices.get(classOf[WorkerLeaseService]).blocking(() => testClassProcessor.stop())
    finally
      val endTime: Long = clock.getCurrentTime
      if groupByFramework then frameworkTests.foreach(testResultProcessor.completed(_, endTime))
      testResultProcessor.completed(RootTest, endTime)
      resultProcessorActor.foreach(_.stop())
      testEnvironment.close()

object TestTask:
  private val testListenerBroadcaster: Field = classOf[AbstractTestTask].getDeclaredField("testListenerBroadcaster")
  testListenerBroadcaster.setAccessible(true)

  private def runningInIntelliJIdea(task: AbstractTestTask): Boolean =
    var result: Boolean = false

    testListenerBroadcaster
      .get(task)
      .asInstanceOf[ListenerBroadcast[TestListener]]
      .visitListeners((testListener: TestListener) =>
        if testListener.getClass.getName == "IJTestEventLogger$1" then result = true
      )

    result

  private def findOnClassPath(name: String): URL =
    Gradle.findOnClassPath(TestTask, name)
