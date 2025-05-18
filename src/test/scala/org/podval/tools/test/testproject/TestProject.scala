package org.podval.tools.test.testproject

import org.gradle.testkit.runner.GradleRunner
import org.podval.tools.build.{Dependency, ScalaBackend}
import org.podval.tools.util.Files
import scala.jdk.CollectionConverters.SeqHasAsJava
import java.io.File

// TODO abstract, calculate overall test failure, and move up to `org.podval.tools.testproject`.
final class TestProject(projectDir: File):
  def writeSources(
    backend: Option[ScalaBackend],
    isTest: Boolean, 
    sources: Seq[SourceFile]
  ): Unit =
    val directory: File = Files.fileSeq(projectDir,
      backend.toSeq.map(_.sourceRoot) ++ Seq(
      "src",
      if isTest then "test" else "main",
      "scala",
      "org",
      "podval",
      "tools",
      "test"
    ))
    
    for sourceFile: SourceFile <- sources do
      val content: String = sourceFile.content
      Files.write(
        file = Files.file(directory, s"${sourceFile.name}.scala"),
        content =
          if content.contains("package ")
          then content
          else s"package ${ForClass.testPackage}\n\n" + content
      )
  
  private def gradleRunner: GradleRunner = GradleRunner.create.withProjectDir(projectDir)

  def test(commandLineIncludeTestNames: Seq[String]): TestResultsRetriever =
    val testsArgument: List[String] =
      if commandLineIncludeTestNames.isEmpty
      then List.empty
      else List("--tests", commandLineIncludeTestNames.mkString("\"", ", ", "\""))

    // To get test results for all backends, I tell Gradle to `--continue` running the build even when a task fails.
    // TODO do it using a setting on the test task, so that the project is runnable stand-alone.
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
  private def root: File = Files.url2file(getClass.getResource("/org/podval/tools/test/testproject/anchor.txt"))
    .getParentFile // testproject
    .getParentFile // test
    .getParentFile // tools
    .getParentFile // podval
    .getParentFile // org
    .getParentFile // test
    .getParentFile // resources
    .getParentFile // build
    .getParentFile
  
  final def writeProject(
    projectName: Seq[String],
    properties: Seq[(String, String)],
    dependencies: Map[String, Seq[Dependency.WithVersion]],
    buildGradleFragments: Seq[String],
  ): TestProject =
    val projectNameString: String = projectName.mkString("-")
    val projectDir: File = Files.file(
      root,
      Seq("build", "test-projects") ++ projectName ++ Seq(projectNameString)*
    )
    val pluginProjectDir: File = root

    if properties.nonEmpty then
      Files.write(Files.file(projectDir, "gradle.properties"),
        properties.map { case (name, value) => s"$name=$value" }.mkString("\n"))

    Files.write(Files.file(projectDir, "settings.gradle"),
      s"""pluginManagement {
         |  repositories {
         |    mavenLocal()
         |    mavenCentral()
         |    gradlePluginPortal()
         |  }
         |}
         |
         |dependencyResolutionManagement {
         |  repositories {
         |    mavenLocal()
         |    mavenCentral()
         |  }
         |}
         |
         |rootProject.name = '$projectNameString'
         |
         |includeBuild '${pluginProjectDir.getAbsolutePath}'
         |""".stripMargin
    )
    
    val dependenciesString: String = dependencies
      .map { case (key: String, dependencies: Seq[Dependency.WithVersion]) => dependencies
        .map((dependency: Dependency.WithVersion) => s"  $key '${dependency.dependencyNotation}'")
        .mkString("\n")
      }
      .mkString("\n")
    
    Files.write(Files.file(projectDir, "build.gradle"),
      s"""plugins {
         |  id 'org.podval.tools.scalajs' version '0.0.0'
         |}
         |
         |// Do not stop on test failures (needed when test tasks for multiple backends need to run):
         |gradle.startParameter.continueOnFailure = true
         |
         |// There is no Java in the project :)
         |project.gradle.startParameter.excludedTaskNames.add('compileJava')
         |
         |dependencies {
         |$dependenciesString
         |}
         |
         |${buildGradleFragments.filter(_.nonEmpty).mkString("\n\n")}
         |""".stripMargin
    )

    TestProject(projectDir)
