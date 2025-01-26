package org.podval.tools.testing.task

import groovy.lang.{Closure, DelegatesTo}
import org.gradle.StartParameter
import org.gradle.api.internal.DocumentationRegistry
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.scala.ScalaBasePlugin
import org.gradle.api.tasks.testing.{AbstractTestTask, Test, TestListener}
import org.gradle.api.tasks.{SourceSet, TaskAction}
import org.gradle.api.Action
import org.gradle.internal.event.ListenerBroadcast
import org.gradle.internal.time.Clock
import org.gradle.internal.work.WorkerLeaseService
import org.gradle.internal.{Actions, Cast}
import org.gradle.util.internal.ConfigureUtil
import org.podval.tools.build.Gradle.*
import org.podval.tools.build.{GradleClassPath, TaskWithSourceSet}
import org.podval.tools.util.Files
import java.io.File
import java.lang.reflect.{Field, Method}
import scala.jdk.CollectionConverters.IterableHasAsScala

// guide: https://docs.gradle.org/current/userguide/java_testing.html
// configuration: https://docs.gradle.org/current/dsl/org.gradle.api.tasks.testing.Test.html

// Note: inherited Test.testsAreNotFiltered() calls Test.noCategoryOrTagOrGroupSpecified(),
// which recognizes only JUnit and TestNG; since I can not override it, I'll just use
// org.gradle.api.tasks.testing.junit.JUnitOptions for my "sbt" TestFrameworkOptions...
abstract class TestTask extends Test with TaskWithSourceSet:
  // To avoid invoking Task.getProject at execution time, some things are done or captured at create or right before execution:
  setGroup(JavaBasePlugin.VERIFICATION_GROUP)
  getDependsOn.add(getProject.getClassesTask(sourceSet))

  private val buildDirectory: File = getProject.getLayout.getBuildDirectory.get.getAsFile
  private val scalaCompile: String = getProject.getScalaCompile(sourceSet).getName
  private val zincClassPath: Iterable[File] = getProject.getConfiguration(ScalaBasePlugin.ZINC_CONFIGURATION_NAME).asScala
  private var analysisFile: Option[File] = None

  // Note: Deferring creation of the test framework by using
  // `getTestFrameworkProperty.convention(project.provider(() => createTestFramework))` did not work out,
  // nor did the approach that would result in creating the test framework twice when it is set explicitly in the build file:
  // `getTestFrameworkProperty.convention(createTestFramework)`.
  // So, I am sticking with pre-applying the test framework at the task creation time;
  // options seem to work...
  // TODO works with Gradle 8.3, but with Gradle 8.4-rc-1, I get:
  // Caused by: org.gradle.api.tasks.TaskInstantiationException: Could not create task of type 'TestTaskScala'.
  // Caused by: java.lang.NoClassDefFoundError: Could not initialize class org.podval.tools.testing.task.TestTask$
  //   at org.podval.tools.testing.task.TestTask.useSbt(TestTask.scala:53)
  //   at org.podval.tools.testing.task.TestTask.<init>(TestTask.scala:35)
  //   at org.podval.tools.testing.task.TestTaskScala.<init>(TestTaskScala.scala:8)
  //   at org.podval.tools.testing.task.TestTaskScala_Decorated.<init>(Unknown Source)
  useSbt()

  final override protected def sourceSetName: String = SourceSet.TEST_SOURCE_SET_NAME

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
    // delayed: not available at the time of the TestFramework construction
    testEnvironment = () => testEnvironment,
    analysisFile = () => analysisFile.get,
    runningInIntelliJIdea = () => TestTask.runningInIntelliJIdea(TestTask.this)
  )

  @TaskAction override def executeTests(): Unit =
    // Since Gradle's Test task manipulates the test framework in its `executeTests()`,
    // best be done with this here, before `super.createTestExecuter()` is called.
    require(getTestFramework.isInstanceOf[TestFramework], s"Only useSbt Gradle test framework is supported by this plugin - not $testFramework!")
    require(isScanForTestClasses, "File-name based test scan is not supported by this plugin, `isScanForTestClasses` must be `true`!")

    // AnalysisDetector needs Zinc classes;
    // if I ever get rid of it, this classpath expansion goes away.
    // Note: when done at creation, causes:
    //   Cannot change dependencies of dependency configuration ':zinc' after it has been resolved.
    GradleClassPath.addTo(this, zincClassPath)

    // Note: scalaCompile.getAnalysisFiles is empty, so I had to hard-code the path:
    analysisFile = Some(Files.file(buildDirectory, s"tmp/scala/compilerAnalysis/$scalaCompile.analysis"))

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
  private val testListenerSubscriptions: Field = classOf[AbstractTestTask].getDeclaredField("testListenerSubscriptions")
  testListenerSubscriptions.setAccessible(true)

  private val broadcastSubscriptionsGet: Method = Class.forName("org.gradle.api.tasks.testing.AbstractTestTask$BroadcastSubscriptions").getDeclaredMethod("get")
  broadcastSubscriptionsGet.setAccessible(true)

  // see https://github.com/JetBrains/intellij-community/blob/master/plugins/gradle/resources/org/jetbrains/plugins/gradle/IJTestLogger.groovy
  def runningInIntelliJIdea(task: AbstractTestTask): Boolean =
    var result: Boolean = false

    broadcastSubscriptionsGet.invoke(
        testListenerSubscriptions.get(task)
      )
      .asInstanceOf[ListenerBroadcast[TestListener]]
      .visitListeners((testListener: TestListener) =>
        if testListener.getClass.getName == "IJTestEventLogger$1" then result = true
      )

    result