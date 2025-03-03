package org.podval.tools.test.task

import org.gradle.api.Action
import org.gradle.api.internal.classpath.ModuleRegistry
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.logging.{LogLevel, Logger, Logging}
import org.gradle.process.internal.worker.{DefaultWorkerProcessBuilder, WorkerProcessBuilder}
import org.podval.tools.build.GradleClassPath
import org.podval.tools.test.TestEnvironment
import org.podval.tools.test.detect.{TestFilter, TestFrameworkDetector}
import org.podval.tools.test.framework.FrameworkDescriptor
import org.podval.tools.test.processor.RunTestClassProcessorFactory
import org.podval.tools.util.Files
import java.io.File
import java.lang.reflect.Field
import java.net.URL
import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava, SetHasAsScala}

class TestFramework(
  logLevelEnabled: LogLevel,
  testFilter: DefaultTestFilter,
  moduleRegistry: ModuleRegistry,
  analysisFile: File,
  testEnvironment: () => TestEnvironment,
  runningInIntelliJIdea: () => Boolean
) extends org.gradle.api.internal.tasks.testing.TestFramework:

  private val logger: Logger = Logging.getLogger(classOf[TestFramework])

  private val options: TestFrameworkOptions = new TestFrameworkOptions
  override def getOptions: TestFrameworkOptions = options

  override def copyWithFilters(newTestFilters: org.gradle.api.tasks.testing.TestFilter): TestFramework =
    this // TODO

  private var detectorOpt: Option[TestFrameworkDetector] = None
  override def getDetector: TestFrameworkDetector =
    if detectorOpt.isEmpty then detectorOpt = Some:
      logger.lifecycle(s"--tests ${testFilter.getCommandLineIncludePatterns}")
      TestFrameworkDetector(
        testEnvironment(),
        analysisFile,
        TestFilter(
          includes = testFilter.getIncludePatterns.asScala.toSet,
          excludes = testFilter.getExcludePatterns.asScala.toSet,
          commandLineIncludes = testFilter.getCommandLineIncludePatterns.asScala.toSet
        )
      )
    detectorOpt.get

  // TODO why am I not getting a call from the CompositeStoppable in the Gradle's Test task even when the tests succeed?
  override def close(): Unit =
    detectorOpt.get.close()
    detectorOpt = None

  override def getProcessorFactory: RunTestClassProcessorFactory = RunTestClassProcessorFactory(
    includeTags = options.getIncludeCategories.asScala.toArray,
    excludeTags = options.getExcludeCategories.asScala.toArray,
    runningInIntelliJIdea = runningInIntelliJIdea(),
    logLevelEnabled = logLevelEnabled
  )

  // I need to make sure that the plugin classes themselves are on the worker's classpath(s).
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
    jars.map(GradleClassPath.findOn(TestFramework, _)) ++
    (
      gradleModules.map(moduleRegistry.getModule) ++
      externalModules.map(moduleRegistry.getExternalModule)
    ).flatMap(_.getImplementationClasspath.getAsURLs.asScala)

  private val implementationClassPathAdditions: List[URL] = classPathAdditions(
    gradleModules = List(),
    externalModules = List(),
    jars = List(
      "org.podval.tools.scalajs",
      // Without this, when running framework tests on Scala 2,.13 I get:
      // java.lang.NoClassDefFoundError: scala/runtime/LazyVals$
      //	at org.podval.tools.test.framework.FrameworkDescriptor.<clinit>(FrameworkDescriptor.scala:36)
      //	at org.podval.tools.test.framework.FrameworkDescriptor$.<clinit>(FrameworkDescriptor.scala:37)
      //	at org.podval.tools.test.TaskDefTestSpec$.makeRunner(TaskDefTestSpec.scala:48)
      //	at org.podval.tools.test.processor.WorkerTestClassProcessor.getRunner$$anonfun$1(WorkerTestClassProcessor.scala:115)
      // when trying to look up FrameworkDescriptor by name when running forked (on JVM, not JS);
      // it does not seem to break anything even on Scala.js and even on Scala 2.12.
      "scala3-library_3"
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
    FrameworkDescriptor.all.toList.flatMap(_.sharedPackages) ++ List(
      // Scala 3 and Scala 2 libraries;
      // when running on Scala 3, both jars themselves are already on the classpath;
      // when running on Scala 2, Scala 2 library jar is already ion the classpath,
      // and Scala 3 library jar is added to the implementation classpath above.
      "scala",

      // "test-interface"; jar itself is already on the classpath
      "sbt.testing",

      // "groovy" external module added to the applicationClassPath
      "org.codehaus.groovy",

      // When plugin jar is added to the application classpath,
      // share only classes needed for the worker...
    )

  override def getUseDistributionDependencies: Boolean = false

  override def getWorkerConfigurationAction: Action[WorkerProcessBuilder] = (builderInterface: WorkerProcessBuilder) =>
    val builder: DefaultWorkerProcessBuilder = builderInterface.asInstanceOf[DefaultWorkerProcessBuilder]

    builder.setImplementationClasspath((
      // TODO [Gradle PR] to get implementation classpath without reflection - or, better still,
      // to introduce a method for adding to it.
      TestFramework.getImplementationClassPath(builder).asScala.toList ++
      implementationClassPathAdditions
    ).asJava)

    builder.applicationClasspath(
      applicationClassPathAdditions.map(Files.url2file).asJava
    )

    builder.sharedPackages(sharedPackages.asJava)

    ()

object TestFramework:
  private def getImplementationClassPath(builder: DefaultWorkerProcessBuilder): java.util.List[URL] =
    val implementationClassPath: Field = classOf[DefaultWorkerProcessBuilder].getDeclaredField("implementationClassPath")
    implementationClassPath.setAccessible(true)
    implementationClassPath
      .get(builder)
      .asInstanceOf[java.util.List[URL]]
