package org.podval.tools.test

import org.gradle.StartParameter
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.DocumentationRegistry
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.logging.{Logger, LogLevel}
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.provider.{Property, SetProperty}
import org.gradle.api.tasks.{Classpath, Input, Optional, SourceSet}
import org.gradle.api.tasks.testing.{AbstractTestTask, Test, TestListener}
import org.gradle.internal.event.ListenerBroadcast
import org.gradle.internal.time.Clock
import org.gradle.internal.work.WorkerLeaseService
import org.opentorah.build.Gradle
import org.opentorah.build.Gradle.*
import org.opentorah.util.Files

import java.io.File
import java.lang.reflect.Field
import scala.jdk.CollectionConverters.*

// guide: https://docs.gradle.org/current/userguide/java_testing.html
// configuration: https://docs.gradle.org/current/dsl/org.gradle.api.tasks.testing.Test.html
abstract class TestTask extends Test:
  setGroup(JavaBasePlugin.VERIFICATION_GROUP)
  getTestFrameworkProperty.set(createTestFramework)

  private def sourceSet: SourceSet = getProject.getSourceSet(SourceSet.TEST_SOURCE_SET_NAME)

  @Classpath final def getRuntimeClassPath: FileCollection = sourceSet.getRuntimeClasspath
  getProject.afterEvaluate((project: Project) =>
    getDependsOn.add(project.getClassesTask(sourceSet))
    ()
  )

  // TODO move the things into the TestFrameworkOptions
  @Input def getIncludeTags: SetProperty[String]
  @Input def getExcludeTags: SetProperty[String]

  private def createTestFramework: TestFramework =
    TestFramework(
      this,
      getModuleRegistry,
      getFilter.asInstanceOf[DefaultTestFilter],
      TestTask.getLogLevelEnabled(getLogger),
      maxWorkerCount
    )

  final override def executeTests(): Unit =
    // TODO verify that :
    // - testFramework is mine
    // - testExecutionSpec.isScanForTestClasses is true

    try
      super.executeTests()
    finally
      // TODO why am I not getting a call from the CompositeStoppable in the Gradle's Test task even when the tests succeed?
      getTestFramework.close()

  final override def createTestExecuter: TestExecuter = TestExecuter(
    workerProcessFactory = getProcessBuilderFactory,
    actorFactory = getActorFactory,
    moduleRegistry = getModuleRegistry,
    workerLeaseService = getServices.get(classOf[WorkerLeaseService]),
    maxWorkerCount = maxWorkerCount,
    clock = getServices.get(classOf[Clock]),
    documentationRegistry = getServices.get(classOf[DocumentationRegistry]),
    testFilter = getFilter.asInstanceOf[DefaultTestFilter]
  )

  protected def canFork: Boolean

  // Note: this is overridden in the ScalaJS TestTask and requires ScalaJS-related classes
  // which are not on the classpath at the time of the construction of this task,
  // so it can not be passed into the constructor of the TestFramework...
  def sourceMapper: Option[SourceMapper]
  def testEnvironment: TestEnvironment

  // TODO maybe this can be passed into the constructor of the TestFramework?
  final def filesToAddToClassPath: Iterable[File] = getProject.getConfiguration(Sbt.configurationName).asScala

  // TODO maybe this can be passed into the constructor of the TestFramework?
  // Note: scalaCompile.getAnalysisFiles is empty, so I had to hard-code the path:
  def analysisFile: File = Files.file(
    directory = getProject.getBuildDir,
    segments = s"tmp/scala/compilerAnalysis/${getProject.getScalaCompile(sourceSet).getName}.analysis"
  )

  def testTagsFilter: TestTagsFilter = TestTagsFilter(
    include = getIncludeTags.get.asScala.toArray,
    exclude = getExcludeTags.get.asScala.toArray
  )

  private def maxWorkerCount: Int = getServices.get(classOf[StartParameter]).getMaxWorkerCount

  // Note: this gets into the TestExecutionSpec
  override def getMaxParallelForks: Int =
    val result: Int = super.getMaxParallelForks
    if (result == 1) || canFork then result else
      getLogger.info(s"Can not fork tests; maxParallelForks setting ($result) ignored", null, null, null)
      1

object TestTask:
  // TODO move into TestFramework
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

