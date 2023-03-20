package org.podval.tools.testing.task

import groovy.lang.{Closure, DelegatesTo}
import org.gradle.StartParameter
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.DocumentationRegistry
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.scala.ScalaBasePlugin
import org.gradle.api.tasks.testing.{AbstractTestTask, Test, TestListener}
import org.gradle.api.tasks.{Classpath, SourceSet, TaskAction}
import org.gradle.api.{Action, Project}
import org.gradle.internal.event.ListenerBroadcast
import org.gradle.internal.time.Clock
import org.gradle.internal.work.WorkerLeaseService
import org.gradle.internal.{Actions, Cast}
import org.gradle.util.internal.ConfigureUtil
import org.opentorah.build.Gradle.*
import org.opentorah.util.Files
import java.io.File
import java.lang.reflect.{Field, Method}
import scala.jdk.CollectionConverters.*

// guide: https://docs.gradle.org/current/userguide/java_testing.html
// configuration: https://docs.gradle.org/current/dsl/org.gradle.api.tasks.testing.Test.html
abstract class TestTask extends Test:
  setGroup(JavaBasePlugin.VERIFICATION_GROUP)

  // Note: Deferring creation of the test framework by using
  // `getTestFrameworkProperty.convention(project.provider(() => createTestFramework))` did not work out,
  // nor did the approach that would result in creating the test framework twice when it is set explicitly in the build file:
  // `getTestFrameworkProperty.convention(createTestFramework)`.
  // So, I am sticking with pre-applying the test framework at the task creation time;
  // options seem to work...
  useSbt()

  // TODO unify with ScalaJSTask?
  private def sourceSet: SourceSet = getProject.getSourceSet(SourceSet.TEST_SOURCE_SET_NAME)
  @Classpath final def getRuntimeClassPath: FileCollection = sourceSet.getRuntimeClasspath
  getProject.afterEvaluate((project: Project) =>
    getDependsOn.add(project.getClassesTask(sourceSet))
    ()
  )

  final def useSbt(@DelegatesTo(classOf[TestFrameworkOptions]) testFrameworkConfigure: Closure[?]): Unit =
    useSbt(ConfigureUtil.configureUsing(testFrameworkConfigure))

  final def useSbt(testFrameworkConfigure: Action[TestFrameworkOptions]): Unit =
    useSbt()
    // TODO private and too short to bother with reflection
    Actions.`with`(Cast.cast(classOf[TestFrameworkOptions], getOptions), testFrameworkConfigure)

  final def useSbt(): Unit = TestTask.useTestFramework(this, createTestFramework)
  private def createTestFramework: TestFramework = TestFramework(
    logLevelEnabled = getServices.get(classOf[StartParameter]).getLogLevel,
    testFilter = getFilter.asInstanceOf[DefaultTestFilter],
    moduleRegistry = getModuleRegistry,
    // delayed: not available at the time of the TestFramework construction (task creation)
    testEnvironment = () => testEnvironment,
    analysisFile = () => analysisFile,
    runningInIntelliJIdea = () => TestTask.runningInIntelliJIdea(TestTask.this)
  )

  private def analysisFile: File =
    // AnalysisDetector needs Zinc classes;
    // if I ever get rid of it, this classpath expansion goes away.
    addToClassPath(this, getProject.getConfiguration(ScalaBasePlugin.ZINC_CONFIGURATION_NAME).asScala)

    // Note: scalaCompile.getAnalysisFiles is empty, so I had to hard-code the path:
    Files.file(
      directory = getProject.getBuildDir,
      segments = s"tmp/scala/compilerAnalysis/${getProject.getScalaCompile(sourceSet).getName}.analysis"
    )

  @TaskAction override def executeTests(): Unit =
    // Since Gradle's Test task manipulates the test framework in its `executeTests()`,
    // best be done with this here, before `super.createTestExecuter()` is called.
    require(getTestFramework.isInstanceOf[TestFramework], s"Only useSbt Gradle test framework is supported by this plugin - not $testFramework!")
    require(isScanForTestClasses, "File-name based test scan is not supported by this plugin, `isScanForTestClasses` must be `true`!")
    super.executeTests()

  final override def createTestExecuter: TestExecuter = TestExecuter(
    canFork = canFork,
    sourceMapper = sourceMapper,
    testFilter = getFilter.asInstanceOf[DefaultTestFilter],
    maxWorkerCount = getServices.get(classOf[StartParameter]).getMaxWorkerCount,
    clock = getServices.get(classOf[Clock]),
    workerProcessFactory = getProcessBuilderFactory,
    actorFactory = getActorFactory,
    workerLeaseService = getServices.get(classOf[WorkerLeaseService]),
    moduleRegistry = getModuleRegistry,
    documentationRegistry = getServices.get(classOf[DocumentationRegistry])
  )

  override def getMaxParallelForks: Int =
    val result: Int = super.getMaxParallelForks
    if canFork then result else 1

  protected def canFork: Boolean

  protected def sourceMapper: Option[SourceMapper]

  protected def testEnvironment: TestEnvironment

object TestTask:
  private val useTestFramework: Method = classOf[Test].getDeclaredMethod("useTestFramework", classOf[org.gradle.api.internal.tasks.testing.TestFramework])
  useTestFramework.setAccessible(true)
  private def useTestFramework(task: Test, value: TestFramework): Unit = useTestFramework.invoke(task, value)

  // TODO Gradle PR: figure this out from the environment - or introduce method to avoid the use of reflection
  private val testListenerBroadcaster: Field = classOf[AbstractTestTask].getDeclaredField("testListenerBroadcaster")
  testListenerBroadcaster.setAccessible(true)

  // see https://github.com/JetBrains/intellij-community/blob/master/plugins/gradle/resources/org/jetbrains/plugins/gradle/IJTestLogger.groovy
  def runningInIntelliJIdea(task: AbstractTestTask): Boolean =
    var result: Boolean = false

    testListenerBroadcaster
      .get(task)
      .asInstanceOf[ListenerBroadcast[TestListener]]
      .visitListeners((testListener: TestListener) =>
        if testListener.getClass.getName == "IJTestEventLogger$1" then result = true
      )

    result