package org.podval.tools.scalajs

import org.gradle.api.tasks.{TaskAction, TaskProvider}
import org.gradle.api.tasks.scala.ScalaCompile
import org.opentorah.util.Files
import org.podval.tools.scalajs.testing.{Output, Task, TestDefinition, TestFramework, TestLogger, Tests}
import org.scalajs.testing.adapter.TestAdapter
import sbt.testing.{Framework, Runner}
import xsbti.compile.{CompileAnalysis, FileAnalysisStore}
import java.io.File
import scala.jdk.CollectionConverters.*

abstract class TestTask[T <: LinkTask.Test](clazz: Class[T]) extends AfterLinkTask[T](clazz):
  setDescription(s"Run ScalaJS${stage.description}")
  setGroup("build")

  private def scalaCompile: ScalaCompile = linkTask
    .classesTask
    .getDependsOn
    .asScala
    .find(_.isInstanceOf[TaskProvider[ScalaCompile]]) // TODO use a Class[] instance...
    .get
    .asInstanceOf[TaskProvider[ScalaCompile]]
    .get

  // TODO get the file from scalaCompile.getAnalysisFiles...
  private def analysisFile: File = Files.file(
    directory = getProject.getBuildDir,
    segments  = s"tmp/scala/compilerAnalysis/${scalaCompile.getName}.analysis"
  )

  private def compileAnalysis: CompileAnalysis = FileAnalysisStore
    .getDefault(analysisFile)
    .get
    .get
    .getAnalysis

  @TaskAction def execute(): Unit =
    val config = TestAdapter.Config()
      .withLogger(jsLogger)

    // Note: based on org.scalajs.sbtplugin.ScalaJSPluginInternal
    val testAdapter: TestAdapter = new TestAdapter(jsEnv, Seq(input), config)
    val frameworkNames: List[List[String]] = TestFramework.all.map(_.implClassNames.toList)
    val frameworkAdapters: List[Option[Framework]] = testAdapter.loadFrameworks(frameworkNames)
    val loadedFrameworks: Map[TestFramework, Framework] = TestFramework.all.zip(frameworkAdapters).collect {
      case (testFramework, Some(framework)) => (testFramework, framework)
    }.toMap

    // Note: based on sbt.Defaults
    val tests: Seq[TestDefinition] = Tests.discover(
      frameworks = loadedFrameworks.values.toList,
      analysis = compileAnalysis,
      log = getLogger
    ) //._1

    val filteredFrameworks: Map[TestFramework, Framework] = TestFramework.filterFrameworks(loadedFrameworks, tests)

    val runners: Map[TestFramework, Runner] =
      for (testFramework: TestFramework, framework: Framework) <- filteredFrameworks
      yield testFramework -> framework.runner(Array.empty[String], Array.empty[String], null: ClassLoader)

    val logger: TestLogger = TestLogger(getLogger)

    val output: Output = Tests(
      frameworks = filteredFrameworks,
      runners = runners,
      tests = tests,
      listeners = Seq(logger),
      log = getLogger
    ).run()

    // getLogger.lifecycle(output.toString)

object TestTask:
  class FastOpt   extends TestTask(classOf[LinkTask.Test.FastOpt  ]) with ScalaJSTask.FastOpt
  class FullOpt   extends TestTask(classOf[LinkTask.Test.FullOpt  ]) with ScalaJSTask.FullOpt
  class Extension extends TestTask(classOf[LinkTask.Test.Extension]) with ScalaJSTask.Extension
