package org.podval.tools.test

import org.gradle.api.internal.tasks.testing.{JvmTestExecutionSpec, TestExecuter, TestResultProcessor}
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.{Classpath, SourceSet}
import org.gradle.api.tasks.testing.{AbstractTestTask, Test, TestListener}
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.internal.event.ListenerBroadcast
import org.opentorah.build.Gradle.*
import org.podval.tools.test.GradleUtil.*
import org.opentorah.util.Files
import java.io.File
import java.lang.reflect.Field
import scala.jdk.CollectionConverters.*

abstract class TestTask extends Test:
  setGroup(JavaBasePlugin.VERIFICATION_GROUP)

  private def sourceSet: SourceSet = getProject.getSourceSet(SourceSet.TEST_SOURCE_SET_NAME)
  @Classpath final def getRuntimeClassPath: FileCollection = sourceSet.getRuntimeClasspath
  getProject.afterEvaluate((project: Project) =>
    getDependsOn.add(project.getClassesTask(sourceSet))
    ()
  )

  protected def testEnvironment: TestEnvironment

  final override def createTestExecuter: TestExecuter[JvmTestExecutionSpec] = new TestExecuter[JvmTestExecutionSpec]:
    override def stopNow(): Unit = () // TODO
    override def execute(
      testExecutionSpec: JvmTestExecutionSpec,
      testResultProcessor: TestResultProcessor
    ): Unit =
      // Note: scalaCompile.getAnalysisFiles is empty, so I had to hard-code the path:
      val testScalaCompileName: String = getProject.getScalaCompile(sourceSet).getName
      val analysisFile: File = Files.file(
        directory = getProject.getBuildDir,
        segments = s"tmp/scala/compilerAnalysis/$testScalaCompileName.analysis"
      )

      val filter: DefaultTestFilter = getFilter.asInstanceOf[DefaultTestFilter]
      def convert(set: java.util.Set[String]): Set[String] = set.asScala.toSet

      // Note: suppress use of colours when running in IntelliJ Idea
      var useColours: Boolean = true
      val testListenerBroadcaster: Field = classOf[AbstractTestTask]
        .getDeclaredField("testListenerBroadcaster")
      testListenerBroadcaster.setAccessible(true)
      testListenerBroadcaster
        .get(TestTask.this)
        .asInstanceOf[ListenerBroadcast[TestListener]]
        .visitListeners((listener: TestListener) =>
          if listener.getClass.getName == "IJTestEventLogger$1" then useColours = false
        )

      TestRunner.run(
        testEnvironment = testEnvironment,
        analysisFile = analysisFile,
        includePatterns = convert(filter.getIncludePatterns),
        excludePatterns = convert(filter.getExcludePatterns),
        commandLineIncludePatterns = convert(filter.getCommandLineIncludePatterns),
        testResultProcessor = testResultProcessor,
        logger = getLogger,
        useColours = useColours
      )
