package org.podval.tools.testing.task

import groovy.lang.{Closure, DelegatesTo}
import org.gradle.StartParameter
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.DocumentationRegistry
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.internal.tasks.testing.{JvmTestExecutionSpec, TestExecuter}
import org.gradle.api.logging.{LogLevel, Logger}
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.scala.ScalaBasePlugin
import org.gradle.api.tasks.testing.{AbstractTestTask, Test, TestListener}
import org.gradle.api.tasks.{Classpath, SourceSet}
import org.gradle.api.{Action, Project}
import org.gradle.internal.event.ListenerBroadcast
import org.gradle.internal.time.Clock
import org.gradle.internal.work.WorkerLeaseService
import org.gradle.internal.{Actions, Cast}
import org.gradle.util.internal.ConfigureUtil
import org.opentorah.build.Gradle.*
import org.opentorah.util.Files
import org.podval.tools.testing.worker.TaskDefTest
import sbt.testing.Framework
import java.io.File
import java.lang.reflect.Method

// guide: https://docs.gradle.org/current/userguide/java_testing.html
// configuration: https://docs.gradle.org/current/dsl/org.gradle.api.tasks.testing.Test.html
abstract class TestTask extends Test:
  setGroup(JavaBasePlugin.VERIFICATION_GROUP)
  useSbt()

  private def sourceSet: SourceSet = getProject.getSourceSet(SourceSet.TEST_SOURCE_SET_NAME)

  @Classpath final def getRuntimeClassPath: FileCollection = sourceSet.getRuntimeClasspath
  getProject.afterEvaluate((project: Project) =>
    getDependsOn.add(project.getClassesTask(sourceSet))
    ()
  )

  final def useSbt(@DelegatesTo(classOf[TestFrameworkOptions]) testFrameworkConfigure: Closure[_]): Unit =
    useSbt(ConfigureUtil.configureUsing(testFrameworkConfigure))

  final def useSbt(testFrameworkConfigure: Action[TestFrameworkOptions]): Unit =
    useSbt()
    // TODO private and too short to bother with reflection
    Actions.`with`(Cast.cast(classOf[TestFrameworkOptions], getOptions), testFrameworkConfigure)

  final def useSbt(): Unit = TestTask.useTestFramework(this, createTestFramework)
  private def createTestFramework: TestFramework =
    TestFramework(
      task = this,
      canFork = canFork,
      logLevelEnabled = TestTask.getLogLevelEnabled(getLogger),
      testFilter = getFilter.asInstanceOf[DefaultTestFilter],
      maxWorkerCount = getServices.get(classOf[StartParameter]).getMaxWorkerCount,
      clock = getServices.get(classOf[Clock]),
      workerProcessFactory = getProcessBuilderFactory,
      actorFactory = getActorFactory,
      workerLeaseService = getServices.get(classOf[WorkerLeaseService]),
      moduleRegistry = getModuleRegistry,
      documentationRegistry = getServices.get(classOf[DocumentationRegistry])
    )

  final override def createTestExecuter: TestExecuter[JvmTestExecutionSpec] =
    require(getTestFramework.isInstanceOf[TestFramework], "Only useSbt test Gradle test framework is supported by this plugin!")
    require(isScanForTestClasses, "File-name based test scan is not supported by this plugin!")
    getTestFramework.asInstanceOf[TestFramework].createTestExecuter

  protected def canFork: Boolean

  override def getMaxParallelForks: Int =
    val result: Int = super.getMaxParallelForks
    if canFork then result else 1

  // Note: this is overridden in the ScalaJS TestTask and requires ScalaJS-related classes
  // which are not on the classpath at the time of the construction of this task,
  // so it can not be passed into the constructor of the TestFramework...
  def sourceMapper: Option[SourceMapper]
  def testEnvironment: TestEnvironment

  final def detectTests(loadedFrameworks: List[Framework]): Seq[TestClass] =
    Util.addConfigurationToClassPath(this, ScalaBasePlugin.ZINC_CONFIGURATION_NAME)

    AnalysisDetector.detectTests(
      loadedFrameworks = loadedFrameworks,
      // Note: scalaCompile.getAnalysisFiles is empty, so I had to hard-code the path:
      analysisFile = Files.file(
        directory = getProject.getBuildDir,
        segments = s"tmp/scala/compilerAnalysis/${getProject.getScalaCompile(sourceSet).getName}.analysis"
      )
    )

object TestTask:
  private val useTestFramework: Method = classOf[Test].getDeclaredMethod("useTestFramework", classOf[org.gradle.api.internal.tasks.testing.TestFramework])
  useTestFramework.setAccessible(true)
  private def useTestFramework(task: Test, value: TestFramework): Unit = useTestFramework.invoke(task, value)

  // TODO replace with Gradle.getLogLevelEnabled(Logger) once opentorah.util is released
  private val levels: Seq[LogLevel] = Seq(
    LogLevel.DEBUG,
    LogLevel.INFO,
    LogLevel.LIFECYCLE,
    LogLevel.WARN,
    LogLevel.QUIET,
    LogLevel.ERROR
  )

  private def getLogLevelEnabled(logger: Logger): LogLevel = levels.find(logger.isEnabled).get

