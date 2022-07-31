package org.podval.tools.test

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.provider.{Property, SetProperty}
import org.gradle.api.tasks.{Classpath, Input, Optional, SourceSet}
import org.gradle.api.tasks.testing.{AbstractTestTask, Test, TestListener}
import org.gradle.internal.event.ListenerBroadcast
import org.gradle.internal.work.WorkerLeaseService
import org.gradle.StartParameter
import org.gradle.api.internal.DocumentationRegistry
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.internal.time.Clock
import org.opentorah.build.Gradle.*
import org.opentorah.util.Files
import java.lang.reflect.Field
import scala.jdk.CollectionConverters.*

// guide: https://docs.gradle.org/current/userguide/java_testing.html
// configuration: https://docs.gradle.org/current/dsl/org.gradle.api.tasks.testing.Test.html
abstract class TestTask extends Test:
  setGroup(JavaBasePlugin.VERIFICATION_GROUP)

  private def sourceSet: SourceSet = getProject.getSourceSet(SourceSet.TEST_SOURCE_SET_NAME)

  @Classpath final def getRuntimeClassPath: FileCollection = sourceSet.getRuntimeClasspath
  getProject.afterEvaluate((project: Project) =>
    getDependsOn.add(project.getClassesTask(sourceSet))
    ()
  )

  @Input @Optional def getGroupByFramework: Property[Boolean]
  @Input def getIncludeTags: SetProperty[String]
  @Input def getExcludeTags: SetProperty[String]

  protected def canFork: Boolean

  protected def sourceMapper: Option[SourceMapper]
  protected def testEnvironment: TestEnvironment

  final override def createTestExecuter: TestExecuter = TestExecuter(
    groupByFramework = getGroupByFramework.toOption.getOrElse(false),
    canFork = canFork,
    sourceMapper = sourceMapper,
    testEnvironment = testEnvironment,
    // Note: scalaCompile.getAnalysisFiles is empty, so I had to hard-code the path:
    scalaCompileAnalysisFile = Files.file(
      directory = getProject.getBuildDir,
      segments = s"tmp/scala/compilerAnalysis/${getProject.getScalaCompile(sourceSet).getName}.analysis"
    ),
    runningInIntelliJIdea = TestTask.runningInIntelliJIdea(this),
    sbtClassPath = getProject.getConfiguration(Sbt.configurationName).asScala,
    workerProcessFactory = getProcessBuilderFactory,
    actorFactory = getActorFactory,
    moduleRegistry = getModuleRegistry,
    workerLeaseService = getServices.get(classOf[WorkerLeaseService]),
    maxWorkerCount = getServices.get(classOf[StartParameter]).getMaxWorkerCount,
    clock = getServices.get(classOf[Clock]),
    documentationRegistry = getServices.get(classOf[DocumentationRegistry]),
    testFiltering = TestFiltering(
      includes = getFilter.getIncludePatterns.asScala.toSet,
      excludes = getFilter.getExcludePatterns.asScala.toSet,
      commandLineIncludes = getFilter.asInstanceOf[DefaultTestFilter].getCommandLineIncludePatterns.asScala.toSet
    ),
    testTagging = TestTagging(
      include = getIncludeTags.get.asScala.toArray,
      exclude = getExcludeTags.get.asScala.toArray
    )
  )

object TestTask:
  private val testListenerBroadcaster: Field = classOf[AbstractTestTask].getDeclaredField("testListenerBroadcaster")
  testListenerBroadcaster.setAccessible(true)

  private def runningInIntelliJIdea(task: AbstractTestTask): Boolean =
    var result: Boolean = false

    testListenerBroadcaster
      .get(task)
      .asInstanceOf[ListenerBroadcast[TestListener]]
      .visitListeners((testListener: TestListener) =>
        if testListener.getClass.getName == "IJTestEventLogger$1" then result = true
      )

    result
