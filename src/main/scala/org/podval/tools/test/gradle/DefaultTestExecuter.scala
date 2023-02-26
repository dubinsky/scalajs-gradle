package org.podval.tools.test.gradle

import com.google.common.collect.ImmutableList
import org.gradle.api.file.FileTree
import org.gradle.api.internal.DocumentationRegistry
import org.gradle.api.internal.classpath.ModuleRegistry
import org.gradle.api.internal.tasks.testing.JvmTestExecutionSpec
import org.gradle.api.internal.tasks.testing.TestClassProcessor
import org.gradle.api.internal.tasks.testing.TestExecuter
import org.gradle.api.internal.tasks.testing.TestFramework
import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.gradle.api.internal.tasks.testing.WorkerTestClassProcessorFactory
import org.gradle.api.internal.tasks.testing.detection.TestFrameworkDetector
import org.gradle.api.internal.tasks.testing.detection.DefaultTestClassScanner
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.internal.tasks.testing.processors.MaxNParallelTestClassProcessor
import org.gradle.api.internal.tasks.testing.processors.PatternMatchTestClassProcessor
import org.gradle.api.internal.tasks.testing.processors.RestartEveryNTestClassProcessor
import org.gradle.api.internal.tasks.testing.processors.RunPreviousFailedFirstTestClassProcessor
import org.gradle.api.internal.tasks.testing.processors.TestMainAction
import org.gradle.api.internal.tasks.testing.worker.ForkingTestClassProcessor
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
import java.util.List
import scala.jdk.CollectionConverters.*

/**
 * The default test class scanner factory.
 */
// TODO switch to extendable Gradle class org.gradle.api.internal.tasks.testing.detection.DefaultTestExecuter once it is released
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

  private var processor: TestClassProcessor = _
  override def stopNow(): Unit = if processor != null then processor.stopNow()

  override def execute(testExecutionSpec: JvmTestExecutionSpec, testResultProcessor: TestResultProcessor): Unit = {
    val testFramework: TestFramework = testExecutionSpec.getTestFramework
    val testInstanceFactory: WorkerTestClassProcessorFactory = testFramework.getProcessorFactory

    // TODO: Loading jars from the Gradle distribution can lead confusion in regards
    // to which test framework dependencies actually end up on the classpath, and can
    // lead to multiple different versions on the classpath at once.
    // Once test suites are de-incubated, we should deprecate this distribution-loading
    // behavior entirely and rely on the tests to always provide their implementation
    // dependencies.

    val (classpath: java.util.List[File], modulePath: java.util.List[File]) =
      if testFramework.getUseDistributionDependencies then
        if testExecutionSpec.getTestIsModule then
          (
            pathWithAdditionalJars(testExecutionSpec.getClasspath, testFramework.getTestWorkerApplicationClasses),
            pathWithAdditionalJars(testExecutionSpec.getModulePath, testFramework.getTestWorkerApplicationModules)
          )
        else
          // For non-module tests, add all additional distribution jars to the classpath.
          val additionalClasspath: java.util.List[String] = ImmutableList.builder[String]()
            .addAll(testFramework.getTestWorkerApplicationClasses)
            .addAll(testFramework.getTestWorkerApplicationModules)
            .build()

          (
            pathWithAdditionalJars(testExecutionSpec.getClasspath, additionalClasspath),
            ImmutableList.copyOf(testExecutionSpec.getModulePath)
          )
      else
        (
          ImmutableList.copyOf(testExecutionSpec.getClasspath),
          ImmutableList.copyOf(testExecutionSpec.getModulePath)
        )

    //    println(testWorkerModulePath.asScala.mkString("----- TestExecuter testWorkerModulePath:\n", "\n", "\n-----"))
    //    println(testWorkerClassPath .asScala.mkString("----- TestExecuter testWorkerClassPath :\n", "\n", "\n-----"))

    val forkingProcessorFactory: Factory[TestClassProcessor] = new Factory[TestClassProcessor]:
      override def create: TestClassProcessor =
        createTestClassProcessor(
          workerLeaseService,
          workerFactory,
          testInstanceFactory,
          testExecutionSpec.getJavaForkOptions,
          classpath,
          modulePath,
          testFramework.getWorkerConfigurationAction,
          moduleRegistry,
          documentationRegistry
        )

    val reforkingProcessorFactory: Factory[TestClassProcessor] = new Factory[TestClassProcessor]:
      override def create: TestClassProcessor = RestartEveryNTestClassProcessor(
        forkingProcessorFactory,
        testExecutionSpec.getForkEvery
      )

    processor =
      PatternMatchTestClassProcessor(testFilter,
        RunPreviousFailedFirstTestClassProcessor(testExecutionSpec.getPreviousFailedTestClasses,
          MaxNParallelTestClassProcessor(getMaxParallelForks(testExecutionSpec), reforkingProcessorFactory, actorFactory)))

    val testClassFiles: FileTree = testExecutionSpec.getCandidateClassFiles

    val testFrameworkDetector: TestFrameworkDetector =
      if !testExecutionSpec.isScanForTestClasses || testFramework.getDetector == null then null else
        val result: TestFrameworkDetector = testFramework.getDetector
        result.setTestClasses(java.util.ArrayList[File](testExecutionSpec.getTestClassesDirs.getFiles))
        result.setTestClasspath(classpath)
        result

    TestMainAction(
      DefaultTestClassScanner(testClassFiles, testFrameworkDetector, processor),
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
    applicationClassPath: java.util.List[File],
    applicationModulePath: java.util.List[File],
    workerConfigurationAction: Action[WorkerProcessBuilder],
    moduleRegistry: ModuleRegistry,
    documentationRegistry: DocumentationRegistry
  ): TestClassProcessor = ForkingTestClassProcessor(
    workerLeaseService,
    workerProcessFactory,
    workerTestClassProcessorFactory,
    javaForkOptions,
    applicationClassPath,
    applicationModulePath,
    workerConfigurationAction,
    moduleRegistry,
    documentationRegistry
  )

  private def getMaxParallelForks(testExecutionSpec: JvmTestExecutionSpec): Int =
    var maxParallelForks: Int = testExecutionSpec.getMaxParallelForks
    if maxParallelForks > maxWorkerCount then
      logger.info("{}.maxParallelForks ({}) is larger than max-workers ({}), forcing it to {}", testExecutionSpec.getPath, maxParallelForks, maxWorkerCount, maxWorkerCount)
      maxParallelForks = maxWorkerCount

    maxParallelForks

  /**
   * Create a classpath or modulePath, as a list of files, given both the files provided by the test spec and a list of
   * modules to load from the Gradle distribution.
   *
   * @param testFiles A set of jars, as given from a {@link JvmTestExecutionSpec}'s classpath or modulePath.
   * @param additionalModules The names of any additional modules to load from the Gradle distribution.
   *
   * @return A set of files representing the constructed classpath or modulePath.
   */
  private def pathWithAdditionalJars[F <: File](
    testFiles: java.lang.Iterable[F],
    additionalModules: java.util.List[String]
  ): java.util.List[File] =
    val outputFiles: ImmutableList.Builder[File] = ImmutableList.builder[File]().addAll(testFiles)

    if logger.isDebugEnabled() && !additionalModules.isEmpty then
      logger.debug("Loaded additional modules from the Gradle distribution: " + additionalModules.asScala.mkString(", "), null, null, null)

    for (module: String <- additionalModules.asScala) do
      outputFiles.addAll(moduleRegistry.getExternalModule(module).getImplementationClasspath.getAsFiles)

      // TODO: The user is relying on dependencies from the Gradle distribution. Emit a deprecation warning.
      // We may want to wait for test-suites to be de-incubated here. If users are using the `test.useJUnitPlatform`
      // syntax, they will need to list their framework dependency manually, but if they are using the
      // `testing.suites.test.useJUnitFramework` syntax, they do not need to explicitly list their dependencies.
      // We don't want to push users to add their dependencies explicitly if test suites will remove that
      // requirement in the future.

    outputFiles.build()

