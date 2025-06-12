package org.podval.tools.test.testproject

import org.gradle.testkit.runner.GradleRunner
import org.podval.tools.backend.ScalaBackend
import org.podval.tools.util.Files
import scala.jdk.CollectionConverters.SeqHasAsJava
import java.io.File

// TODO abstract, calculate overall test failure, and move up to `org.podval.tools.testproject`.
final class TestProject(projectName: Seq[String]):
  val projectNameString: String = projectName.mkString("-")
  val pluginProjectDir: File = TestProject.root
  val projectDir: File = Files.file(
    TestProject.root,
    Seq("build", "test-projects") ++ projectName ++ Seq(projectNameString)*
  )

  def writer(backend: Option[ScalaBackend]): TestProjectWriter = TestProjectWriter(backend match
    case None => projectDir
    case Some(backend) => File(projectDir, backend.sourceRoot)
  )
  
  private def gradleRunner: GradleRunner = GradleRunner.create.withProjectDir(projectDir)

  def test(commandLineIncludeTestNames: Seq[String]): TestResultsRetriever =
    val testsArgument: List[String] =
      if commandLineIncludeTestNames.isEmpty
      then List.empty
      else List("--tests", commandLineIncludeTestNames.mkString("\"", ", ", "\""))
    
    val testOutput: String = gradleRunner
      .withArguments((List("clean", "test", "-i") ++ testsArgument).asJava)
      .forwardOutput
      .buildAndFail
      .getOutput

    TestResultsRetriever(projectDir)

  def run: String = gradleRunner
    .withArguments("run")
    .build
    .getOutput

object TestProject:
  def root: File = Files.url2file(getClass.getResource("/org/podval/tools/test/testproject/anchor.txt"))
    .getParentFile // testproject
    .getParentFile // test
    .getParentFile // tools
    .getParentFile // podval
    .getParentFile // org
    .getParentFile // test
    .getParentFile // resources
    .getParentFile // build
    .getParentFile
