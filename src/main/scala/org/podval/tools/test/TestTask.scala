package org.podval.tools.test

import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.{Classpath, Input, SourceSet}
import org.gradle.api.tasks.testing.{AbstractTestTask, Test, TestListener}
import org.gradle.api.file.FileCollection
import org.gradle.internal.event.ListenerBroadcast
import org.gradle.internal.work.WorkerLeaseService
import org.gradle.StartParameter
import org.gradle.api.internal.DocumentationRegistry
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.plugins.scala.ScalaBasePlugin
import org.gradle.internal.time.Clock
import org.opentorah.build.Gradle.*
import org.opentorah.util.Files
import java.io.File
import java.lang.reflect.Field
import javax.inject.Inject
import scala.jdk.CollectionConverters.*

abstract class TestTask extends Test:
  setGroup(JavaBasePlugin.VERIFICATION_GROUP)

  private def sourceSet: SourceSet = getProject.getSourceSet(SourceSet.TEST_SOURCE_SET_NAME)

  @Classpath final def getRuntimeClassPath: FileCollection = sourceSet.getRuntimeClasspath
  getProject.afterEvaluate((project: Project) =>
    getDependsOn.add(project.getClassesTask(sourceSet))
    ()
  )

  @Input def getIncludeTags: SetProperty[String]
  @Input def getExcludeTags: SetProperty[String]

  protected def testClassPath: Array[File]
  protected def sourceMapper: Option[SourceMapper]
  protected def testEnvironment: TestEnvironment

  final override def createTestExecuter: TestExecuter =
    val testFilter: DefaultTestFilter = getFilter.asInstanceOf[DefaultTestFilter]
    TestExecuter(
      testClassPath = testClassPath,
      sourceMapper = sourceMapper,
      testEnvironment = testEnvironment,
      scalaCompileAnalysisFile = scalaCompileAnalysisFile,
      runningInIntelliJIdea = runningInIntelliJIdea,
      sbtClassPath = getProject.getConfiguration(Sbt.configurationName).asScala,
      workerProcessFactory = getProcessBuilderFactory,
      actorFactory = getActorFactory,
      moduleRegistry = getModuleRegistry,
      workerLeaseService = getServices.get(classOf[WorkerLeaseService]),
      maxWorkerCount = getServices.get(classOf[StartParameter]).getMaxWorkerCount,
      clock = getServices.get(classOf[Clock]),
      documentationRegistry = getServices.get(classOf[DocumentationRegistry]),
      includes = testFilter.getIncludePatterns.asScala.toSet,
      excludes = testFilter.getExcludePatterns.asScala.toSet,
      commandLineIncludes = testFilter.getCommandLineIncludePatterns.asScala.toSet,
      includeTags = getIncludeTags.get.asScala.toArray,
      excludeTags = getExcludeTags.get.asScala.toArray,
      serviceRegistry = getServices
    )

  // Note: scalaCompile.getAnalysisFiles is empty, so I had to hard-code the path:
  private def scalaCompileAnalysisFile: File =
    val testScalaCompileName: String = getProject.getScalaCompile(sourceSet).getName
    Files.file(
      directory = getProject.getBuildDir,
      segments = s"tmp/scala/compilerAnalysis/$testScalaCompileName.analysis"
    )

  private def runningInIntelliJIdea: Boolean =
    var result: Boolean = false

    TestTask.testListenerBroadcaster
      .get(TestTask.this)
      .asInstanceOf[ListenerBroadcast[TestListener]]
      .visitListeners((testListener: TestListener) =>
        if testListener.getClass.getName == "IJTestEventLogger$1" then result = true
      )

    result

object TestTask:
  private val testListenerBroadcaster: Field = classOf[AbstractTestTask].getDeclaredField("testListenerBroadcaster")
  testListenerBroadcaster.setAccessible(true)
