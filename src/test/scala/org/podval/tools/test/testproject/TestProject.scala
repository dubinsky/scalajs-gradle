package org.podval.tools.test.testproject

import org.gradle.testkit.runner.GradleRunner
import org.podval.tools.build.ScalaBackend
import org.podval.tools.platform.Files
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

  private def gradleRunner: GradleRunner = GradleRunner
    .create
    .withProjectDir(projectDir)
    .withGradleVersion("8.14.3")

  def test(commandLineIncludeTestNames: Seq[String]): TestResultsRetriever =
    // With ZIO Test on Scala.js and Scala Native, I get:
    //  The Daemon will expire immediately since the JVM garbage collector is thrashing.
    //  The currently configured max heap space is '512 MiB' and the configured max metaspace is '384 MiB'.
    // Settings from ~/.gradle/gradle.properties do not have effect when using GradleRunner,
    // and anyway, I need to bump the memory for running on the GitHub CI...
    val javaArgument: Seq[String] = List("-Dorg.gradle.jvmargs='-Xmx16g'") // "-XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError"

    val testsArgument: List[String] =
      if commandLineIncludeTestNames.isEmpty
      then List.empty
      else List("--tests", commandLineIncludeTestNames.mkString("\"", ", ", "\""))

    val testOutput: String = gradleRunner
      .withArguments((javaArgument ++ List("clean", "test", "-i") ++ testsArgument).asJava)
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
