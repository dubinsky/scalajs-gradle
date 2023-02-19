package org.podval.tools.test

import org.gradle.api.Action
import org.gradle.api.internal.classpath.ModuleRegistry
import org.gradle.api.internal.DocumentationRegistry
import org.gradle.api.internal.tasks.testing.{JvmTestExecutionSpec, TestClassProcessor, TestResultProcessor}
import org.gradle.api.internal.tasks.testing.detection.DefaultTestClassScanner
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.internal.tasks.testing.processors.{MaxNParallelTestClassProcessor, PatternMatchTestClassProcessor,
  RestartEveryNTestClassProcessor, RunPreviousFailedFirstTestClassProcessor}
import org.gradle.api.logging.{Logger, Logging}
import org.gradle.internal.actor.ActorFactory
import org.gradle.internal.Factory
import org.gradle.internal.time.Clock
import org.gradle.internal.serialize.SerializerRegistry
import org.gradle.internal.work.WorkerLeaseService
import org.gradle.process.internal.worker.{WorkerProcessBuilder, WorkerProcessContext, WorkerProcessFactory}
import org.gradle.process.JavaForkOptions
import java.io.File
import scala.jdk.CollectionConverters.*

// TODO switch to extendable Gradle classes once they are released -
// and delete mine ;)
class TestExecuter(
  workerProcessFactory: WorkerProcessFactory,
  actorFactory: ActorFactory,
  moduleRegistry: ModuleRegistry,
  workerLeaseService: WorkerLeaseService,
  maxWorkerCount: Int,
  clock: Clock,
  documentationRegistry: DocumentationRegistry,
  testFilter: DefaultTestFilter
) extends
  // TODO switch to extendable Gradle class once it is released
  org.podval.tools.test.gradle
//  org.gradle.api.internal.tasks.testing.detection
  .DefaultTestExecuter(
    workerProcessFactory,
    actorFactory,
    moduleRegistry,
    workerLeaseService,
    maxWorkerCount,
    clock,
    documentationRegistry,
    testFilter
  ):

  private val logger: Logger = Logging.getLogger(classOf[TestExecuter])
  private var testClassProcessorOpt: Option[TestClassProcessor] = None

  override def stopNow(): Unit = testClassProcessorOpt.foreach(_.stopNow())

  override protected def createTestClassProcessor(
    workerLeaseService: WorkerLeaseService,
    workerProcessFactory: WorkerProcessFactory,
    workerTestClassProcessorFactory: org.gradle.api.internal.tasks.testing.WorkerTestClassProcessorFactory,
    javaForkOptions: JavaForkOptions,
    applicationClassPath: java.util.List[File],
    applicationModulePath: java.util.List[File],
    workerConfigurationAction: Action[WorkerProcessBuilder],
    moduleRegistry: ModuleRegistry,
    documentationRegistry: DocumentationRegistry
  ): TestClassProcessor = new
    // TODO switch to extendable Gradle class once it is released
    org.podval.tools.test.gradle
    //org.gradle.api.internal.tasks.testing.worker
    .ForkingTestClassProcessor(
      workerLeaseService,
      workerProcessFactory,
      workerTestClassProcessorFactory,
      javaForkOptions,
      applicationClassPath,
      applicationModulePath,
      workerConfigurationAction,
      moduleRegistry,
      documentationRegistry
    ):
      override protected def createParameterSerializers: SerializerRegistry = TestSerializerRegistry.create

      override protected def createTestWorker(
        processorFactory: org.gradle.api.internal.tasks.testing.WorkerTestClassProcessorFactory
      ): Action[WorkerProcessContext] = new
        // TODO switch to extendable Gradle class once it is released
          org.podval.tools.test.gradle
          //org.gradle.api.internal.tasks.testing.worker
          .TestWorker(processorFactory):
        override protected def createParameterSerializers: SerializerRegistry = TestSerializerRegistry.create

  override def execute(
    testExecutionSpec: JvmTestExecutionSpec,
    testResultProcessor: TestResultProcessor
  ): Unit =
    val testFramework: TestFramework = testExecutionSpec.getTestFramework.asInstanceOf[TestFramework]
    // TODO verify testExecutionSpec.isScanForTestClasses == true
    testFramework.withTestExecutionSpec(testExecutionSpec)

    val testResultProcessorEffective: TestResultProcessor =
      SourceMappingTestResultProcessor(
        TracingTestResultProcessor(
          testResultProcessor,
          clock,
          isEnabled = true
        ),
        testFramework.sourceMapper
      )

    // TODO uncomment when the rest is commented out
    // Once guava ImmutableList issue is resolved, remove the commented code!
    //super.execute(testExecutionSpec, testResultProcessorEffective)

    val testInstanceFactory: WorkerTestClassProcessorFactory = testFramework.getProcessorFactory

    val (testWorkerClassPath, testWorkerModulePath) =
      if testFramework.getUseDistributionDependencies then
        if testExecutionSpec.getTestIsModule then
          (
            pathWithAdditionalJars(testExecutionSpec.getClasspath .asScala.toList, testFramework.getTestWorkerApplicationClasses),
            pathWithAdditionalJars(testExecutionSpec.getModulePath.asScala.toList, testFramework.getTestWorkerApplicationModules)
          )
        else
          // For non-module tests, add all additional distribution jars to the classpath.
          val additionalClasspath: java.util.List[String] =
            (testFramework.getTestWorkerApplicationClasses.asScala ++ testFramework.getTestWorkerApplicationModules.asScala).asJava

          (
            pathWithAdditionalJars(testExecutionSpec.getClasspath.asScala.toList, additionalClasspath),
            testExecutionSpec.getModulePath.asScala.toList.asJava
          )
      else
        (
          testExecutionSpec.getClasspath.asScala.toList.asJava,
          testExecutionSpec.getModulePath.asScala.toList.asJava
        )

    // TODO this is a *serious* deviation from the DefaultTestExecuter,
    // but ScalaJS tests do not work when forking/reforking is in the way...
    // This should be resolved by tweaking more classloaders,
    // because there is no clean way to blend this in as an overridable method,
    // and besides, I do not want the test report to change,
    // *and* I do not want to copy the WorkerSuit-wrapping code either!
    val maxParallelForks: Int = getMaxParallelForks(testExecutionSpec)
    val testClassProcessorRaw: TestClassProcessor =
      if maxParallelForks == 1 then testInstanceFactory.create(clock) else
        val forkingProcessorFactory: Factory[TestClassProcessor] = () =>
          createTestClassProcessor(
            workerLeaseService = workerLeaseService,
            workerProcessFactory = workerProcessFactory,
            workerTestClassProcessorFactory = testInstanceFactory,
            javaForkOptions = testExecutionSpec.getJavaForkOptions,
            applicationClassPath = testWorkerClassPath,
            applicationModulePath = testWorkerModulePath,
            workerConfigurationAction = testFramework.getWorkerConfigurationAction,
            moduleRegistry = moduleRegistry,
            documentationRegistry = documentationRegistry
          )

        val reForkingProcessorFactory: Factory[TestClassProcessor] = () =>
          RestartEveryNTestClassProcessor(
            forkingProcessorFactory,
            testExecutionSpec.getForkEvery
          )

        MaxNParallelTestClassProcessor(
          maxParallelForks,
          reForkingProcessorFactory,
          actorFactory
        )

    val testClassProcessor: TestClassProcessor =
      PatternMatchTestClassProcessor(testFilter,
        RunPreviousFailedFirstTestClassProcessor(testExecutionSpec.getPreviousFailedTestClasses,
          testClassProcessorRaw
        )
      )

    testClassProcessorOpt = Some(testClassProcessor)

    org.gradle.api.internal.tasks.testing.processors.TestMainAction(
      DefaultTestClassScanner(
        testExecutionSpec.getCandidateClassFiles,
        testFramework.getDetector,
        testClassProcessor
      ),
      testClassProcessor,
      testResultProcessorEffective,
      workerLeaseService,
      clock,
      testExecutionSpec.getPath,
      s"Gradle Test Run ${testExecutionSpec.getIdentityPath}"
    ).run()

  private def getMaxParallelForks(testExecutionSpec: JvmTestExecutionSpec): Int =
    var maxParallelForks: Int = testExecutionSpec.getMaxParallelForks
    if maxParallelForks > maxWorkerCount then
      logger.info("{}.maxParallelForks ({}) is larger than max-workers ({}), forcing it to {}", testExecutionSpec.getPath, maxParallelForks, maxWorkerCount, maxWorkerCount)
      maxParallelForks = maxWorkerCount

    maxParallelForks

  // TODO: The user is relying on dependencies from the Gradle distribution. Emit a deprecation warning.
  // We may want to wait for test-suites to be de-incubated here. If users are using the `test.useJUnitPlatform`
  // syntax, they will need to list their framework dependency manually, but if they are using the
  // `testing.suites.test.useJUnitFramework` syntax, they do not need to explicitly list their dependencies.
  // We don't want to push users to add their dependencies explicitly if test suites will remove that
  // requirement in the future.
  private def pathWithAdditionalJars(
    testFiles: List[File],
    additionalModules: java.util.List[String]
  ): java.util.List[File] =
    if logger.isDebugEnabled && !additionalModules.isEmpty then
      logger.debug("Loaded additional modules from the Gradle distribution: " + additionalModules.asScala.mkString(", "), null, null, null)
    val testFilesList: List[File] = testFiles
    val additionalModulesList: List[File] =
      additionalModules.asScala.toList.flatMap(moduleRegistry.getExternalModule(_).getImplementationClasspath.getAsFiles.asScala.toList)
    (testFilesList ++ additionalModulesList).asJava
