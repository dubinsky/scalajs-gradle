package org.podval.tools.test.task

import groovy.lang.{Closure, DelegatesTo}
import org.gradle.StartParameter
import org.gradle.api.Action
import org.gradle.api.internal.DocumentationRegistry
import org.gradle.api.internal.tasks.testing.TestFramework
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.{Input, Internal, SourceSet, TaskAction}
import org.gradle.internal.time.Clock
import org.gradle.internal.work.WorkerLeaseService
import org.gradle.internal.{Actions, Cast}
import org.gradle.util.internal.ConfigureUtil
import org.podval.tools.build.Gradle
import org.podval.tools.test.environment.TestEnvironment
import org.podval.tools.util.Files
import java.io.File
import java.lang.reflect.Method

// guide: https://docs.gradle.org/current/userguide/java_testing.html
// configuration: https://docs.gradle.org/current/dsl/org.gradle.api.tasks.testing.Test.html
abstract class TestTask extends Test:
  setGroup(JavaBasePlugin.VERIFICATION_GROUP)
  getDependsOn.add(Gradle.getClassesTask(getProject, sourceSetName))
  private def sourceSetName: String = SourceSet.TEST_SOURCE_SET_NAME

  // TODO move into SbtTestFrameworkOptions
  @Input def getUseSbtAnalysisTestDetection: Property[java.lang.Boolean]
  getUseSbtAnalysisTestDetection.convention(false)

  useSbt()

  final def useSbt(@DelegatesTo(classOf[SbtTestFrameworkOptions]) testFrameworkConfigure: Closure[?]): Unit =
    useSbt(ConfigureUtil.configureUsing(testFrameworkConfigure))

  final def useSbt(testFrameworkConfigure: Action[SbtTestFrameworkOptions]): Unit =
    useSbt()
    Actions.`with`(Cast.cast(classOf[SbtTestFrameworkOptions], getOptions), testFrameworkConfigure)

  final def useSbt(): Unit = TestTask.useTestFramework(this, sbtTestFramework)

  private lazy val sbtTestFramework: SbtTestFramework =
    // `scalaCompile.getAnalysisFiles` is empty, so I had to hard-code the path:
    val analysisFile: File = Files.file(
      getProject.getLayout.getBuildDirectory.get.getAsFile,
      s"tmp/scala/compilerAnalysis/${Gradle.getScalaCompile(getProject, sourceSetName).getName}.analysis"
    )

    SbtTestFramework(
      isScalaJS = isScalaJS,
      logLevelEnabled = getServices.get(classOf[StartParameter]).getLogLevel,
      defaultTestFilter = getFilter.asInstanceOf[DefaultTestFilter],
      options = SbtTestFrameworkOptions(),
      moduleRegistry = getModuleRegistry,
      testTaskTemporaryDir = getTemporaryDirFactory,
      dryRun = getDryRun,
      useSbtAnalysisTestDetection = getUseSbtAnalysisTestDetection,
      analysisFile = analysisFile,
      // delayed: not available at the time of the TestFramework construction (task creation)
      testEnvironmentGetter = () => testEnvironment,
      runningInIntelliJIdea = () => IntelliJIdea.runningIn(TestTask.this)
    )

  private lazy val testEnvironment: TestEnvironment = createTestEnvironment
  protected def createTestEnvironment: TestEnvironment

  // Since Gradle's Test task manipulates the test framework in its `executeTests()`,
  // best be done with this here, before `super.createTestExecuter()` is called.
  @TaskAction override def executeTests(): Unit =
    require(getTestFramework.isInstanceOf[SbtTestFramework],
      s"Only `useSbt` Gradle test framework is supported by this plugin - not $testFramework!")
    require(isScanForTestClasses,
      "File-name based test scan is not supported by this plugin, `isScanForTestClasses` must be `true`!")

    super.executeTests()

//    testEnvironment.close()

  final override def createTestExecuter: TestExecuter = TestExecuter(
    isScalaJS = isScalaJS,
    sourceMapper = testEnvironment.sourceMapper,
    testFilter = getFilter.asInstanceOf[DefaultTestFilter],
    maxWorkerCount = getServices.get(classOf[StartParameter]).getMaxWorkerCount,
    clock = getServices.get(classOf[Clock]),
    workerProcessFactory = getProcessBuilderFactory,
    actorFactory = getActorFactory,
    workerLeaseService = getServices.get(classOf[WorkerLeaseService]),
    moduleRegistry = getModuleRegistry,
    documentationRegistry = getServices.get(classOf[DocumentationRegistry])
  )

  @Internal protected def isScalaJS: Boolean

  final override def getMaxParallelForks: Int =
    if isScalaJS
    then 1 
    else super.getMaxParallelForks

object TestTask:
  private def useTestFramework(task: Test, value: TestFramework): Unit =
    val useTestFramework: Method = classOf[Test].getDeclaredMethod("useTestFramework", classOf[TestFramework])
    useTestFramework.setAccessible(true)
    useTestFramework.invoke(task, value)
