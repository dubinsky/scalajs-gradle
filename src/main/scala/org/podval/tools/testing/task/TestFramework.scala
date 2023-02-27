package org.podval.tools.testing.task

import org.gradle.api.Action
import org.gradle.api.internal.DocumentationRegistry
import org.gradle.api.internal.classpath.{Module, ModuleRegistry}
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.internal.tasks.testing.worker.ForkedTestClasspath
import org.gradle.api.internal.tasks.testing.{JvmTestExecutionSpec, TestClassProcessor, TestExecuter, TestResultProcessor,
  WorkerTestClassProcessorFactory}
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.testing.{AbstractTestTask, TestListener}
import org.gradle.internal.actor.ActorFactory
import org.gradle.internal.event.ListenerBroadcast
import org.gradle.internal.serialize.SerializerRegistry
import org.gradle.internal.time.Clock
import org.gradle.internal.work.WorkerLeaseService
import org.gradle.process.JavaForkOptions
import org.gradle.process.internal.worker.{DefaultWorkerProcessBuilder, WorkerProcessBuilder, WorkerProcessFactory}
import org.opentorah.build.Gradle
import org.opentorah.util.Files
import org.podval.tools.testing.framework.FrameworkDescriptor
import org.podval.tools.testing.processors.{NonForkingTestClassProcessor, TaskDefTestEncodingTestClassProcessor}
import org.podval.tools.testing.results.{FixUpRootTestOutputTestResultProcessor, SourceMappingTestResultProcessor,
  TracingTestResultProcessor}
import org.podval.tools.testing.worker.TestTagsFilter
import sbt.testing.Framework
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

  private val options: TestFrameworkOptions = new TestFrameworkOptions
  override def getOptions: TestFrameworkOptions = options

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
    if detectorOpt.isEmpty then detectorOpt = Some {
      val loadedFrameworks: List[Framework] = getTestEnvironment.loadAllFrameworks
      // The rest of the code assumes that the Framework is uniquely identified by its name:
      require(loadedFrameworks.map(_.name).size == loadedFrameworks.size, "Different frameworks with the same name!")
      TestFrameworkDetector(task.detectTests(loadedFrameworks), TestFilter(testFilter))
    }
    detectorOpt.get

  // TODO why am I not getting a call from the CompositeStoppable in the Gradle's Test task even when the tests succeed?
  override def close(): Unit =
    detectorOpt = None
    testEnvironmentOpt.foreach(_.close())
    ()

  // Note: capture the parameter needed for the getProcessorFactory() call in the execute() call
  private var testExecutionSpecOpt: Option[JvmTestExecutionSpec] = None

  override def getProcessorFactory: WorkerTestClassProcessorFactory =
    // Note: just the local test classes/resources
    val testClassPath: Array[URL] = testExecutionSpecOpt
      .get
      .getClasspath
      .asScala
      .filterNot(_.getName.endsWith(".jar"))
      .map(Files.file2url)
      .toArray

    org.podval.tools.testing.worker.TestClassProcessor.Factory(
      testClassPath = testClassPath,
      testTagsFilter = TestTagsFilter(
        include = options.getIncludeTags.asScala.toArray,
        exclude = options.getExcludeTags.asScala.toArray
      ),
      runningInIntelliJIdea = TestFramework.runningInIntelliJIdea(task),
      logLevelEnabled = logLevelEnabled
    )

  def createTestExecuter: TestExecuter[JvmTestExecutionSpec] = new DefaultTestExecuter(
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
      testExecutionSpecOpt = Some(testExecutionSpec)

      // Note: deeper down, TestMainAction wraps testResultProcessorEffective in AttachParentTestResultProcessor.
      val testResultProcessorEffective: TestResultProcessor =
        FixUpRootTestOutputTestResultProcessor(
          SourceMappingTestResultProcessor(
            TracingTestResultProcessor(
              testResultProcessor,
              clock,
              isEnabled = false
            ),
            task.sourceMapper
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
      // ScalaJS tests must be run in the same JVM where they are discovered.
      val doNotFork: Boolean = !canFork // TODO provide a way to not fork Scala tests
      if doNotFork then
        NonForkingTestClassProcessor(
          workerTestClassProcessorFactory,
          clock
        )
      else
        // Encoding of TaskDefTest happens at the end of the TestClassProcessor chain,
        // so that PatternMatchTestClassProcessor and RunPreviousFailedFirstTestClassProcessor
        // can do their jobs.
        TaskDefTestEncodingTestClassProcessor(
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

  // TODO [classpath] I need to make sure that the plugin classes themselves are on the worker's classpath(s).
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
  //
  // I really need only the classes that the worker uses,
  // but I think the trouble starts once I add the jar to the application classpath,
  // even if I share nothing; if not, I can just segregate those classes in a package and share it,
  // but splitting the jar is a pain...
  private def classPathAdditions(
    gradleModules: List[String],
    externalModules: List[String],
    jars: List[String]
  ): List[URL] =
    jars.map(Gradle.findOnClassPath(TestFramework, _)) ++
    (
      gradleModules.map(moduleRegistry.getModule) ++
      externalModules.map(moduleRegistry.getExternalModule)
    ).flatMap(_.getImplementationClasspath.getAsURLs.asScala)

  private val implementationClassPathAdditions: List[URL] = classPathAdditions(
    gradleModules = List(),
    externalModules = List(),
    jars = List(
      "org.podval.tools.scalajs"
    )
  )

  private val applicationClassPathAdditions: List[URL] = classPathAdditions(
    gradleModules = List(),
    externalModules = List(
      // Without this, starting with Gradle 7.6 I get:
      //   java.lang.NoClassDefFoundError: org/codehaus/groovy/runtime/callsite/CallSite
      "groovy"
    ),
    jars = List()
  )

  private val sharedPackages: List[String] =
    // testing framework jars themselves are already on the classpath
    FrameworkDescriptor.all.flatMap(_.sharedPackages) ++ List(
      // Scala 3 and Scala 2 libraries; jars themselves are already on the classpath
      "scala",

      // "test-interface"; jar itself is already on the classpath
      "sbt.testing",

      // "groovy" external module added to the applicationClassPath
      "org.codehaus.groovy",

      // TODO [classpath] when plugin jat is added to the application classpath:
      // share only classes needed for the worker:
      //      "org.podval.tools.testing.exceptions",
      //      "org.podval.tools.testing.framework",
      //      "org.podval.tools.testing.serializer",
      //      "org.podval.tools.testing.worker",
    )

  override def getUseDistributionDependencies: Boolean = false

  override def getWorkerConfigurationAction: Action[WorkerProcessBuilder] = (builderInterface: WorkerProcessBuilder) =>
    val builder: DefaultWorkerProcessBuilder = builderInterface.asInstanceOf[DefaultWorkerProcessBuilder]

    // TODO Gradle PR: DefaultWorkerProcessBuilder.implementationClasspath(Iterable[File])
    builder.setImplementationClasspath((
      TestFramework.getImplementationClassPath(builder).asScala.toList ++
      implementationClassPathAdditions
    ).asJava)

    builder.applicationClasspath(
      applicationClassPathAdditions.map(Files.url2file).asJava
    )

    builder.sharedPackages(sharedPackages.asJava)

    ()

object TestFramework:
  // TODO Gradle PR: introduce method to avoid the use of reflection
  private val testListenerBroadcaster: Field = classOf[AbstractTestTask].getDeclaredField("testListenerBroadcaster")
  testListenerBroadcaster.setAccessible(true)

  def runningInIntelliJIdea(task: AbstractTestTask): Boolean =
    var result: Boolean = false

    testListenerBroadcaster
      .get(task)
      .asInstanceOf[ListenerBroadcast[TestListener]]
      .visitListeners((testListener: TestListener) =>
        // see https://github.com/JetBrains/intellij-community/blob/master/plugins/gradle/resources/org/jetbrains/plugins/gradle/IJTestLogger.groovy
        if testListener.getClass.getName == "IJTestEventLogger$1" then result = true
      )

    result

  // TODO Gradle PR: introduce method to avoid the use of reflection
  private val implementationClassPath: Field = classOf[DefaultWorkerProcessBuilder].getDeclaredField("implementationClassPath")
  implementationClassPath.setAccessible(true)

  private def getImplementationClassPath(builder: DefaultWorkerProcessBuilder): java.util.List[URL] =
    implementationClassPath
      .get(builder)
      .asInstanceOf[java.util.List[URL]]
