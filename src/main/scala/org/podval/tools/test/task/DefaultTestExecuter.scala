package org.podval.tools.test.task

import org.gradle.api.file.FileTree
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
import java.lang.reflect.{InvocationTargetException, Method}

// Translated and improved org.gradle.api.internal.tasks.testing.detection.DefaultTestExecuter.
// This is the only Gradle class that I need to fork, modify and maintain
// (to use NonForkingTestClassProcessor needed for Scala.js tests).
class DefaultTestExecuter(
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

  private var testClassProcessor: Option[TestClassProcessor] = None
  override def stopNow(): Unit = testClassProcessor.foreach(_.stopNow())

  override def execute(testExecutionSpec: JvmTestExecutionSpec, testResultProcessor: TestResultProcessor): Unit =
    val testFramework: TestFramework = testExecutionSpec.getTestFramework
    val testInstanceFactory: WorkerTestClassProcessorFactory = testFramework.getProcessorFactory

    val classpath: ForkedTestClasspath =
      createForkedTestClasspathCompat(testClasspathFactory, testExecutionSpec, testFramework)

    val forkingProcessorFactory: Factory[TestClassProcessor] = new Factory[TestClassProcessor]:
      override def create: TestClassProcessor =
        createTestClassProcessor(
          workerLeaseService,
          workerFactory,
          testInstanceFactory,
          testExecutionSpec.getJavaForkOptions,
          classpath,
          testFramework.getWorkerConfigurationAction
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
      if !testExecutionSpec.isScanForTestClasses || testFramework.getDetector == null then None else Some:
        val result: TestFrameworkDetector = testFramework.getDetector
        result.setTestClasses(java.util.ArrayList[File](testExecutionSpec.getTestClassesDirs.getFiles))
        val applicationClasspath: java.util.List[File] = classpath
          .getClass
          .getMethod("getApplicationClasspath")
          .invoke(classpath)
          .asInstanceOf[java.util.List[File]]
        result.setTestClasspath(applicationClasspath)
        result

    TestMainAction(
      DefaultTestClassScanner(testClassFiles, testFrameworkDetector.orNull, processor),
      processor,
      testResultProcessor,
      workerLeaseService,
      clock,
      testExecutionSpec.getPath,
      "Gradle Test Run " + testExecutionSpec.getIdentityPath
    ).run()

  protected def createTestClassProcessor(
    workerLeaseService: WorkerLeaseService,
    workerProcessFactory: WorkerProcessFactory,
    workerTestClassProcessorFactory: WorkerTestClassProcessorFactory,
    javaForkOptions: JavaForkOptions,
    classpath: ForkedTestClasspath,
    workerConfigurationAction: Action[WorkerProcessBuilder]
  ): TestClassProcessor = ForkingTestClassProcessor(
    workerLeaseService,
    workerProcessFactory,
    workerTestClassProcessorFactory,
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

  // Tries new 2-arg create, then old 4-arg create. Uses reflection to avoid hard API dependency.
  private def createForkedTestClasspathCompat(
                                               factory: ForkedTestClasspathFactory,
                                               spec: JvmTestExecutionSpec,
                                               framework: TestFramework
                                             ): ForkedTestClasspath =
    val argsNew: Array[Object] = Array(
      spec.getClasspath.asInstanceOf[Object],
      spec.getModulePath.asInstanceOf[Object]
    )
    val argsOld: Array[Object] = Array(
      spec.getClasspath.asInstanceOf[Object],
      spec.getModulePath.asInstanceOf[Object],
      framework.asInstanceOf[Object],
      java.lang.Boolean.valueOf(spec.getTestIsModule).asInstanceOf[Object]
    )

    // Prefer new signature first
    tryInvokeCreate(factory, 2, argsNew)
      .orElse {
        tryInvokeCreate(factory, 4, argsOld)
      }
      .getOrElse {
        val methods = factory.getClass.getMethods.filter(_.getName == "create").map(_.toString).mkString("\n  - ", "\n  - ", "")
        throw new NoSuchMethodException(
          s"Could not invoke ForkedTestClasspathFactory.create with either new or old signatures." +
            s"\nTried 2 and 4 parameters." +
            s"\nAvailable create methods:${methods}"
        )
      }

  private def tryInvokeCreate(
                               factory: ForkedTestClasspathFactory,
                               paramCount: Int,
                               args: Array[Object]
                             ): Option[ForkedTestClasspath] =
    val candidates = factory.getClass.getMethods.filter(m => m.getName == "create" && m.getParameterCount == paramCount)
    candidates.view
      .map { m =>
        try
          Some(m.invoke(factory, args*).asInstanceOf[ForkedTestClasspath])
        catch
          case _: IllegalArgumentException => None
          case _: InvocationTargetException => None
          case _: ClassCastException => None
          case _: ReflectiveOperationException => None
      }
      .collectFirst { case Some(v) => v }
