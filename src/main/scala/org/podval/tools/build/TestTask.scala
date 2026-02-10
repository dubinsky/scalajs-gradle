package org.podval.tools.build

import groovy.lang.{Closure, DelegatesTo}
import org.gradle.StartParameter
import org.gradle.api.internal.tasks.testing.TestFramework
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.tasks.{CacheableTask, TaskAction}
import org.gradle.api.tasks.testing.Test
import org.gradle.api.{Action, Project}
import org.gradle.internal.time.Clock
import org.gradle.internal.work.WorkerLeaseService
import org.gradle.internal.{Actions, Cast}
import org.gradle.util.internal.ConfigureUtil
import org.podval.tools.test.detect.SbtTestFramework
import org.podval.tools.test.run.TestExecuter
import org.podval.tools.util.{Reflection, Tasks}

// guide: https://docs.gradle.org/current/userguide/java_testing.html
// configuration: https://docs.gradle.org/current/dsl/org.gradle.api.tasks.testing.Test.html
// TODO add ScalaLibrary/ScalaVersion here and use it in loading frameworks
object TestTask:
  def configureTasks[T <: TestTask[?]](
    project: Project,
    testTaskClass: Class[T]
  ): Unit = Tasks.configureEach(
    project,
    testTaskClass,
    (testTask: T) =>
      testTask.setGroup(Tasks.verificationGroup)
      testTask.useSbt()
  )

@CacheableTask
abstract class TestTask[B <: Backend] extends Test
  with Backend.Task[B]
  with OutputTask:

  protected def testEnvironmentCreator: TestEnvironment.Creator[B]

  final def useSbt(@DelegatesTo(classOf[SbtTestFramework.Options]) testFrameworkConfigure: Closure[?]): Unit =
    useSbt(ConfigureUtil.configureUsing(testFrameworkConfigure))

  final def useSbt(testFrameworkConfigure: Action[SbtTestFramework.Options]): Unit =
    useSbt()
    Actions.`with`(Cast.cast(classOf[SbtTestFramework.Options], getOptions), testFrameworkConfigure)

  final def useSbt(): Unit = Reflection.Invoke[Unit, Test]("useTestFramework", classOf[TestFramework])(this, sbtTestFramework)
  
  private var testEnvironment: Option[TestEnvironment[B]] = None

  private def getTestEnvironment: TestEnvironment[B] =
    if testEnvironment.isEmpty then testEnvironment = Some(testEnvironmentCreator.testEnvironment)
    testEnvironment.get

  private def closeTestEnvironment(): Unit =
    if testEnvironment.isDefined then
      testEnvironment.get.close()
      testEnvironment = None

  private lazy val sbtTestFramework: SbtTestFramework = SbtTestFramework(
    defaultTestFilter = getFilter.asInstanceOf[DefaultTestFilter],
    options = SbtTestFramework.Options(),
    moduleRegistry = getModuleRegistry,
    testTaskTemporaryDir = getTemporaryDirFactory,
    dryRun = getDryRun,
    // delayed: not available at the time of the TestFramework construction (task creation)
    testEnvironment = () => getTestEnvironment,
    output = () => output
  )
  
  // Since Gradle's Test task manipulates the test framework in its `executeTests()`,
  // best be done with this here, before `super.createTestExecuter()` is called.
  @TaskAction final override def executeTests(): Unit =
    require(getTestFramework.isInstanceOf[SbtTestFramework],
      s"Only `useSbt` Gradle test framework is supported by this plugin - not $getTestFramework!")
    require(isScanForTestClasses,
      "File-name based test scan is not supported by this plugin, `isScanForTestClasses` must be `true`!")

    // close and discard testEnvironment even if some tests failed - lest we run out of memory ;)
    try
      super.executeTests()
    finally
      closeTestEnvironment()

  final override def createTestExecuter: TestExecuter = TestExecuter(
    testsCanNotBeForked = getTestEnvironment.backend.testsCanNotBeForked,
    sourceMapper = getTestEnvironment.sourceMapper,
    testFilter = getFilter.asInstanceOf[DefaultTestFilter],
    maxWorkerCount = getServices.get(classOf[StartParameter]).getMaxWorkerCount,
    clock = getServices.get(classOf[Clock]),
    workerProcessFactory = getProcessBuilderFactory,
    actorFactory = getActorFactory,
    workerLeaseService = getServices.get(classOf[WorkerLeaseService]),
    moduleRegistry = getModuleRegistry
  )

  final override def getMaxParallelForks: Int = 
    if getTestEnvironment.backend.testsCanNotBeForked
    then 1
    else super.getMaxParallelForks
