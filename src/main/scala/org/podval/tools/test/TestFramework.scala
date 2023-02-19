package org.podval.tools.test

import org.gradle.api.Action
import org.gradle.api.internal.classpath.{Module, ModuleRegistry}
import org.gradle.api.internal.tasks.testing.{JvmTestExecutionSpec, TestClassProcessor}
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.logging.LogLevel
import org.gradle.process.internal.worker.{DefaultWorkerProcessBuilder, WorkerProcessBuilder}
import org.opentorah.build.Gradle
import org.opentorah.util.Files
import org.podval.tools.test.framework.FrameworkDescriptor
import java.io.File
import java.lang.reflect.Field
import java.net.URL
import scala.jdk.CollectionConverters.*

class TestFramework(
  task: TestTask,
  moduleRegistry: ModuleRegistry,
  testFilter: DefaultTestFilter,
  logLevelEnabled: LogLevel,
  maxWorkerCount: Int,
) extends org.gradle.api.internal.tasks.testing.TestFramework:

  override def copyWithFilters(newTestFilters: org.gradle.api.tasks.testing.TestFilter): TestFramework =
    this // TODO

  // Note: when TestFramework constructor gets called, classPath does not yet have ScalaJS classes,
  // so this gets retrieved from the task
  private var testEnvironmentOpt: Option[TestEnvironment] = None
  lazy val sourceMapper: Option[SourceMapper] = task.sourceMapper

  override def getDetector: TestFrameworkDetector =
    if testEnvironmentOpt.isEmpty then testEnvironmentOpt = Some(task.testEnvironment)
    TestFrameworkDetector(
      filesToAddToClassPath = task.filesToAddToClassPath,
      loadedFrameworks = testEnvironmentOpt.get.loadAllFrameworks,
      analysisFile = task.analysisFile,
      testFilter = TestFilter(testFilter)
    )

  override def close(): Unit =
    // TODO dispose of the detector
    testEnvironmentOpt.foreach(_.close())
    ()

  override def getOptions: TestFrameworkOptions =
    new TestFrameworkOptions // TODO move my options into this

  // Note: this back-channel delivers the parameter needed for the getProcessorFactory() call
  private var testExecutionSpecOpt: Option[JvmTestExecutionSpec] = None
  def withTestExecutionSpec(value: JvmTestExecutionSpec): Unit = testExecutionSpecOpt = Some(value)
  override def getProcessorFactory: WorkerTestClassProcessorFactory =
    val testExecutionSpec: JvmTestExecutionSpec = testExecutionSpecOpt.get
    val maxParallelForks: Int = Math.min(testExecutionSpec.getMaxParallelForks, maxWorkerCount)
    WorkerTestClassProcessorFactory(
      isForked = maxParallelForks > 1,
      runningInIntelliJIdea = TestTask.runningInIntelliJIdea(task),
      testClassPath = testExecutionSpec.getClasspath.asScala.toArray,
      testTagsFilter = task.testTagsFilter,
      logLevelEnabled = logLevelEnabled,
      rootTestSuiteId = testExecutionSpec.getPath // code duplication with the DefaultTestExecuter
    )

  // Note: here I make sure that my classes are on the worker's classpath(s);
  // it would be nice to add what I need as modules, but as far as I can tell,
  // those are only looked up in some Gradle module registry.
  // testExecutionSpec.getClasspath contains the testing frameworks.

  // The only thing remaining is to figure out to which classpath to add what I need
  // (application or implementation) and how to share it so that I stop getting
  // ClassNotFound but do not start getting CanNotCast ;)

  // nothing added: CNF org.podval.tools.test.gradle.TestWorker
  //   add org.podval.tools.scalajs to the applicationClassPath: same
  //     add org.podval.tools.test to sharedPackages: CNF org.gradle.api.Action
  //       it seems that Gradle classes from the implementation classpath
  //       are not available on the application classpath
  //       XXX
  //   add org.podval.tools.scalajs to the implementationClassPath: CNF scala.CanEqual
  //     add scala3-library and scala-library to the implementationClassPath: CNF sbt.testing.Fingerprint
  //       add test-interface to the applicationClassPath and sbt.testing to the sharedPackages: CNF org.scalatest.tools.Framework
  //         add org.scalatest.tools to the sharedPackages

  // TODO look into this some more - maybe some of this (e.g., groovy) can be handled via normal mechanisms
  // like getTestWorkerApplicationClasses()

  private val implementationClassPathAdditions: TestFramework.ClassPathAdditions = TestFramework.ClassPathAdditions(
    gradleModules = List(
    ),
    externalModules = List(
      // Note: without this, starting with Gradle 7.6 I get
      //   java.lang.NoClassDefFoundError: org/codehaus/groovy/runtime/callsite/CallSite
      "groovy"
    ),
    jars = List(
      "org.podval.tools.scalajs",
      "scala3-library",
      "scala-library"
    ),
    modulePath = List(
    )
  )

  private val applicationClassPathAdditions: TestFramework.ClassPathAdditions = TestFramework.ClassPathAdditions(
    gradleModules = List(
    ),
    externalModules = List(
    ),
    jars = List(
      // TODO "frameworks" test project works without this, but "scala-only" does not, although both are pure Scala...
      // TODO "scala-only" fails to run any tests: ScalaTest throws IllegalArgumentException!!!
      "test-interface"
    ),
    modulePath = List(
    )
  )

  private val sharedPackages: List[String] =
    List(
      "sbt.testing"
    ) ++
    FrameworkDescriptor.all.flatMap(_.sharedPackages)

  override def getUseDistributionDependencies: Boolean = true
  override def getTestWorkerApplicationClasses: java.util.List[String] = applicationClassPathAdditions.externalModules.asJava
  override def getTestWorkerApplicationModules: java.util.List[String] = applicationClassPathAdditions.modulePath.asJava

  override def getWorkerConfigurationAction: Action[WorkerProcessBuilder] = (builderInterface: WorkerProcessBuilder) =>
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
