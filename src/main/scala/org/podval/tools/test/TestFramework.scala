package org.podval.tools.test

import org.gradle.api.Action
import org.gradle.api.internal.DocumentationRegistry
import org.gradle.api.internal.classpath.{Module, ModuleRegistry}
import org.gradle.api.internal.tasks.testing.{JvmTestExecutionSpec, TestClassProcessor, TestResultProcessor}
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.logging.LogLevel
import org.gradle.internal.actor.ActorFactory
import org.gradle.internal.id.CompositeIdGenerator.CompositeId
import org.gradle.internal.serialize.SerializerRegistry
import org.gradle.internal.time.Clock
import org.gradle.internal.work.WorkerLeaseService
import org.gradle.process.JavaForkOptions
import org.gradle.process.internal.worker.{DefaultWorkerProcessBuilder, WorkerProcessBuilder, WorkerProcessContext,
  WorkerProcessFactory}
import org.opentorah.build.Gradle
import org.opentorah.util.Files
import org.podval.tools.test.framework.FrameworkDescriptor
import java.io.File
import java.lang.reflect.Field
import java.net.URL
import scala.jdk.CollectionConverters.*

class TestFramework(
  task: TestTask,
  canFork: Boolean,
  logLevelEnabled: LogLevel,
  testFilter: DefaultTestFilter,
  maxWorkerCount: Int,
  clock: Clock,
  workerProcessFactory: WorkerProcessFactory,
  actorFactory: ActorFactory,
  workerLeaseService: WorkerLeaseService,
  moduleRegistry: ModuleRegistry,
  documentationRegistry: DocumentationRegistry
) extends org.gradle.api.internal.tasks.testing.TestFramework:

  override def copyWithFilters(newTestFilters: org.gradle.api.tasks.testing.TestFilter): TestFramework =
    this // TODO

  // Note: when TestFramework constructor gets called, classPath does not yet have ScalaJS classes,
  // so this gets retrieved from the task
  private var testEnvironmentOpt: Option[TestEnvironment] = None
  private def getTestEnvironment: TestEnvironment =
    if testEnvironmentOpt.isEmpty then testEnvironmentOpt = Some(task.testEnvironment)
    testEnvironmentOpt.get

  private var detectorOpt: Option[TestFrameworkDetector] = None

  override def getDetector: TestFrameworkDetector =
    if detectorOpt.isEmpty then detectorOpt = Some(
      TestFrameworkDetector(
        filesToAddToClassPath = task.filesToAddToClassPath,
        loadedFrameworks = getTestEnvironment.loadAllFrameworks,
        analysisFile = task.analysisFile,
        testFilter = TestFilter(testFilter)
      )
    )
    detectorOpt.get

  override def close(): Unit =
    detectorOpt = None
    testEnvironmentOpt.foreach(_.close())
    ()

  override def getOptions: TestFrameworkOptions =
    new TestFrameworkOptions // TODO move my options into this

  // Note: capture the parameter needed for the getProcessorFactory() call in the execute() call
  private var testExecutionSpecOpt: Option[JvmTestExecutionSpec] = None

  override def getProcessorFactory: WorkerTestClassProcessorFactory =
    WorkerTestClassProcessorFactory(
      isForked = canFork,
      runningInIntelliJIdea = TestTask.runningInIntelliJIdea(task),
      // TODO augment as in the Action below?
      testClassPath = testExecutionSpecOpt.get.getClasspath.asScala.toArray,
      testTagsFilter = task.testTagsFilter,
      logLevelEnabled = logLevelEnabled,
      rootTestSuiteId = testExecutionSpecOpt.get.getPath // code duplication with the DefaultTestExecuter
    )

  def createTestExecuter: org.podval.tools.test.gradle.DefaultTestExecuter = new org.podval.tools.test.gradle.DefaultTestExecuter(
    workerProcessFactory,
    actorFactory,
    moduleRegistry,
    workerLeaseService,
    maxWorkerCount,
    clock = clock,
    documentationRegistry,
    testFilter
  ):
    override def execute(
      testExecutionSpec: JvmTestExecutionSpec,
      testResultProcessor: TestResultProcessor
    ): Unit =
      // TODO verify testExecutionSpec.isScanForTestClasses == true
      testExecutionSpecOpt = Some(testExecutionSpec)

      val testResultProcessorEffective: TestResultProcessor =
        SourceMappingTestResultProcessor(
          TracingTestResultProcessor(
            testResultProcessor,
            clock,
            isEnabled = false
          ),
          task.sourceMapper
        )

      super.execute(testExecutionSpec, testResultProcessorEffective)

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
    ): org.gradle.api.internal.tasks.testing.TestClassProcessor =
    // TODO ScalaJS tests must be run in the same JVM where they are discovered,
    // so I need to make sure nothing is forked.
    // This is a *serious* deviation from the DefaultTestExecuter -
    // unless I can push it down into the createTestClassProcessor()?
      if !canFork then
        // Note: emulating the nesting of the tests in suits like in the
        // TestWorker.startReceivingTests() called from TestWorker.execute()
        // that is used by the ForkingTestClassProcessor...
        // TODO call start/completed on the suite!
        // TODO factor out into NonForkingWorkerTestClassProcessor
        val workerSuiteId: AnyRef = CompositeId(0L, 0L)
        val workerDisplayName: String = "Non-forking WorkerTestClassProcessor"
        org.gradle.api.internal.tasks.testing.worker.WorkerTestClassProcessor(
          // TODO pass in a registry with the clock?
          workerTestClassProcessorFactory.asInstanceOf[WorkerTestClassProcessorFactory].create(clock),
          workerSuiteId,
          workerDisplayName,
          clock
        )
      else
        new
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

  // TODO
  //  ApplicationClassesInSystemClassLoaderWorkerImplementationFactory
  //  DefaultWorkerProcessBuilder

  // TODO I need to make sure that the plugin classes themselves are on the worker's classpath(s).
  // If I add "org.podval.tools.scalajs" jar to the *implementation* classpath everything works,
  // but feels unclean (and I have to use reflection to do it).
  //
  // If I add the jar to the *application* classpath, I start getting ClassNotFoundException and have to:
  // - add Gradle modules to the application classpath:
  //   "gradle-base-services",    // Action
  //   "gradle-testing-base",     // RemoteTestClassProcessor
  //   "gradle-worker-processes", // WorkerProcessContext
  //   "gradle-messaging",        // SerializerRegistry
  //   "gradle-logging-api",      // LogLevel
  //   "gradle-logging",          // OutputEventListener
  //   "gradle-process-services", // JvmMemoryStatusListener
  // - share "org.gradle" packages with the implementation classpath
  // - add external modules to the application classpath:
  //   "slf4j-api",               // org.slf4j.LoggerFactory
  // and after all that I still get ClassNotFoundException for:
  //   org.gradle.internal.logging.text.StyledTextOutput
  //   org.gradle.internal.nativeintegration.console.ConsoleMetaData
  // ... so this does not seem worth it :(

  private val implementationClassPathAdditions: TestFramework.ClassPathAdditions = TestFramework.ClassPathAdditions(
    gradleModules = List(
    ),
    externalModules = List(
    ),
    jars = List(
      "org.podval.tools.scalajs"
    ),
    modulePath = List(
    )
  )

  private val applicationClassPathAdditions: TestFramework.ClassPathAdditions = TestFramework.ClassPathAdditions(
    gradleModules = List(
    ),
    externalModules = List(
      // Without this, starting with Gradle 7.6 I get:
      //   java.lang.NoClassDefFoundError: org/codehaus/groovy/runtime/callsite/CallSite
      "groovy"
    ),
    jars = List(
      // TODO the jar should be on the classpath, but if I do not add it here, scala-only test project breaks...
      // I guess some test frameworks bring it in and some do not, but I have it as an explicit dependency!!!
      "test-interface"
    ),
    modulePath = List(
    )
  )

  private val sharedPackages: List[String] =
    List(
      // Scala 3 and Scala 2 libraries; jars themselves are already on the classpath
      "scala",

      // "test-interface"; jar itself is already on the classpath
      "sbt.testing",

      // "groovy" external module added to the applicationClassPath
      "org.codehaus.groovy",

      // TODO I probably do not need this, but would if the jar was added to the application classpath...
      "org.podval.tools.test",
      "org.podval.tools.scalajs"
    ) ++
    // testing framework jars themselves are already on the classpath
    FrameworkDescriptor.all.flatMap(_.sharedPackages)

  override def getUseDistributionDependencies: Boolean = true
  override def getTestWorkerApplicationClasses: java.util.List[String] = applicationClassPathAdditions.externalModules.asJava
  override def getTestWorkerApplicationModules: java.util.List[String] = applicationClassPathAdditions.modulePath.asJava

  override def getWorkerConfigurationAction: Action[WorkerProcessBuilder] = (builderInterface: WorkerProcessBuilder) =>
//    println(testExecutionSpecOpt.get.getModulePath.asScala.mkString("----- TestFramework modulePath:\n", "\n", "\n-----"))
//    println(testExecutionSpecOpt.get.getClasspath .asScala.mkString("----- TestFramework classPath :\n", "\n", "\n-----"))

    def findOnClassPath(names: List[String]): List[URL] = names.map(Gradle.findOnClassPath(TestFramework, _))
    def getGradleModules(names: List[String]): List[URL] = toUrls(names.map(moduleRegistry.getModule))
    def getExternalModules(names: List[String]): List[URL] = toUrls(names.map(moduleRegistry.getExternalModule))
    def toUrls(modules: List[Module]): List[URL] = modules.flatMap(_.getImplementationClasspath.getAsURLs.asScala)
    def toFiles(urls: List[URL]): List[File] = urls.map(Files.url2file)

    val builder: DefaultWorkerProcessBuilder = builderInterface.asInstanceOf[DefaultWorkerProcessBuilder]

    // TODO Gradle PR: gimme get() - or DefaultWorkerProcessBuilder.implementationClasspath(Iterable[File])
    builder.setImplementationClasspath((
      TestFramework.getImplementationClassPath(builder).asScala.toList ++
      getGradleModules  (implementationClassPathAdditions.gradleModules  ) ++
      getExternalModules(implementationClassPathAdditions.externalModules) ++
      findOnClassPath   (implementationClassPathAdditions.jars)
    ).asJava)

    // TODO adding assuming that to empty...
    builder.setImplementationModulePath(
      getExternalModules(implementationClassPathAdditions.modulePath).asJava
    )

    builder.applicationClasspath(toFiles(
      getGradleModules(applicationClassPathAdditions.gradleModules) ++
      findOnClassPath(applicationClassPathAdditions.jars)
    ).asJava)

    builder.sharedPackages(sharedPackages.asJava)

    ()

object TestFramework:
  private final class ClassPathAdditions(
    val gradleModules: List[String],
    val externalModules: List[String],
    val jars: List[String],
    val modulePath: List[String]
  )

  // TODO Gradle PR: introduce method to avoid the use of reflection
  private val implementationClassPath: Field = classOf[DefaultWorkerProcessBuilder].getDeclaredField("implementationClassPath")
  implementationClassPath.setAccessible(true)

  private def getImplementationClassPath(builder: DefaultWorkerProcessBuilder): java.util.List[URL] =
    implementationClassPath
      .get(builder)
      .asInstanceOf[java.util.List[URL]]
