package org.podval.tools.test.testproject

import org.gradle.api.Action
import org.gradle.api.internal.tasks.testing.junit.result.{TestClassResult, TestMethodResult, TestResultSerializer}
import org.gradle.testkit.runner.GradleRunner
import org.podval.tools.build.{Dependency, JavaDependency, ScalaBackend, ScalaPlatform, ScalaVersion, Version}
import org.podval.tools.test.framework.FrameworkDescriptor
import org.podval.tools.test.Sbt
import org.podval.tools.util.Files
import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava}
import java.io.File

final class TestProject(projectDir: File):
  private def gradleRunner: GradleRunner = GradleRunner.create().withProjectDir(projectDir)

  def test(commandLineIncludeTestNames: Seq[String]): (List[TestClassResult], String) =
    val testsArgument: List[String] =
      if commandLineIncludeTestNames.isEmpty
      then List.empty
      else List("--tests", commandLineIncludeTestNames.mkString("\"", ", ", "\""))

    // Note: I assume that there are failing tests among the tests being tested
    val testOutput: String = gradleRunner
      .withArguments((List("clean", "test") ++ testsArgument).asJava)
      .buildAndFail
      .getOutput

    val testResults: List[TestClassResult] = TestProject.readTestClassResults(projectDir)

    (testResults, testOutput)

  def run: String = gradleRunner
    .withArguments("run")
    .build
    .getOutput

object TestProject:
  private def readTestClassResults(projectDir: File): List[TestClassResult] =
    val binaryTestReportDir: File = Files.file(projectDir, "build", "test-results", "test", "binary")
    val testResults: TestResultSerializer = TestResultSerializer(binaryTestReportDir)
    var classResults: List[TestClassResult] = List.empty
    val visitor: Action[TestClassResult] = (result: TestClassResult) => classResults = result +: classResults
    testResults.read(visitor)
    classResults

  private def dumpTestClassResults(results: List[TestClassResult]): List[String] = (
    for result: TestClassResult <- results yield
      val classSummary: String = s"${result.getId}: ${result.getClassName} failed=${result.getFailuresCount} skipped=${result.getSkippedCount}"
      val methodResults: List[String] = for result: TestMethodResult <- result.getResults.asScala.toList yield
        s"  ${result.getId}: ${result.getName} resultType=${result.getResultType}"
      List(classSummary) ++ methodResults
    ).flatten

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

  private def scalaFile(projectDir: File, isTest: Boolean, name: String): File = Files.file(
    projectDir,
    "src",
    if isTest then "test" else "main",
    "scala",
    "org",
    "podval",
    "tools",
    "testing",
    s"$name.scala"
  )

  final def writeProject(
    projectName: Seq[String],
    platform: ScalaPlatform,
    mainSources: Seq[SourceFile],
    testSources: Seq[SourceFile],
    frameworks: Seq[FrameworkDescriptor],
    includeTestNames: Seq[String],
    excludeTestNames: Seq[String],
    includeTags: Seq[String],
    excludeTags: Seq[String],
    maxParallelForks: Int,
    mainClassName: Option[String]
  ): TestProject =
    val projectNameString: String = projectName.mkString("-")
    val projectDir: File = Files.file(
      root,
      Seq("build", "test-projects") ++ projectName ++ Seq(projectNameString)*
    )

    val isScalaJS: Boolean = platform.backend.isJS
    
    Files.write(Files.file(projectDir, "gradle.properties"),
      gradleProperties(!isScalaJS))

    Files.write(Files.file(projectDir, "settings.gradle"),
      settingsGradle(projectNameString, pluginProjectDir = root))

    writeSources(projectDir, mainSources, isTest = false)
    writeSources(projectDir, testSources, isTest = true)
    
    Files.write(Files.file(projectDir, "build.gradle"),
      buildGradle(
        nodeVersion = platform.backend match
          case ScalaBackend.JS(nodeVersion) => Some(nodeVersion)
          case _ => None,
        scalaLibraryDependency = platform.version.scalaLibraryDependency.withVersion(platform.scalaVersion),
        zincDependency = Sbt.Zinc.dependencyWithVersion(platform),
        frameworks = frameworks.map((framework: FrameworkDescriptor) =>
          require(framework.isSupported(platform))
          framework.dependencyWithVersion(platform)
        ),
        includeTestNames = includeTestNames,
        excludeTestNames = excludeTestNames,
        includeTags = includeTags,
        excludeTags = excludeTags,
        maxParallelForks = maxParallelForks,
        link = if !isScalaJS then "" else link(mainClassName)
      )
    )

    TestProject(projectDir)
  
  private def writeSources(projectDir: File, sources: Seq[SourceFile], isTest: Boolean): Unit =
    for sourceFile: SourceFile <- sources do
      val content: String = sourceFile.content
      val contentEffective: String =
        if content.contains("package ")
        then content
        else s"package ${ForClass.testPackage}\n\n" + content

      Files.write(scalaFile(projectDir, isTest, sourceFile.name), contentEffective)

  private def gradleProperties(isScalaJSDisabled: Boolean): String =
    s"""org.podval.tools.scalajs.disabled=$isScalaJSDisabled
       |""".stripMargin

  private def settingsGradle(projectName: String, pluginProjectDir: File): String =
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
       |rootProject.name = '$projectName'
       |
       |includeBuild '${pluginProjectDir.getAbsolutePath}'
       |""".stripMargin

  private def buildGradle(
    nodeVersion: Option[Version],
    scalaLibraryDependency: Dependency.WithVersion,
    zincDependency: Dependency.WithVersion,
    frameworks: Seq[Dependency.WithVersion],
    includeTestNames: Seq[String],
    excludeTestNames: Seq[String],
    includeTags: Seq[String],
    excludeTags: Seq[String],
    maxParallelForks: Int,
    link: String
  ): String =
    val nodeVersionString: String = nodeVersion.fold("")((nodeVersion: Version) => s"node.version = '$nodeVersion'\n")
    val includeTestNamesString: String = includeTestNames.map(name => s"    includeTestsMatching '$name'").mkString("\n")
    val excludeTestNamesString: String = excludeTestNames.map(name => s"    excludeTestsMatching '$name'").mkString("\n")
    val includeTagsString: String = includeTags.map(string => s"'$string'").mkString("[", ", ", "]")
    val excludeTagsString: String = excludeTags.map(string => s"'$string'").mkString("[", ", ", "]")

    val frameworksString: String = (
      for framework: Dependency.WithVersion <- frameworks yield
        s"  testImplementation '${framework.dependencyNotation}'"
      ).mkString("\n")

    s"""plugins {
       |  id 'org.podval.tools.scalajs' version '0.0.0'
       |}
       |
       |// There is no Java in the project :)
       |project.gradle.startParameter.excludedTaskNames.add('compileJava')
       |
       |dependencies {
       |  zinc '${zincDependency.dependencyNotation}'
       |  implementation '${scalaLibraryDependency.dependencyNotation}'
       |$frameworksString
       |}
       |
       |$nodeVersionString
       |$link
       |
       |test {
       |  filter {
       |$includeTestNamesString
       |$excludeTestNamesString
       |  }
       |  testLogging.lifecycle {
       |    events("STARTED", "PASSED", "SKIPPED", "FAILED", "STANDARD_OUT", "STANDARD_ERROR")
       |  }
       |  useSbt {
       |    includeCategories = $includeTagsString
       |    excludeCategories = $excludeTagsString
       |  }
       |  maxParallelForks = $maxParallelForks
       |}
       |""".stripMargin
    
  private def link(mainClassName: Option[String]): String =
    val moduleInitializerString: String = mainClassName.fold("")(moduleInitializer)
    s"""link {
       |  optimization     = 'Full'          // one of: 'Fast', 'Full'
       |  moduleKind       = 'NoModule'      // one of: 'NoModule', 'ESModule', 'CommonJSModule'
       |  moduleSplitStyle = 'FewestModules' // one of: 'FewestModules', 'SmallestModules'
       |  prettyPrint      = false
       |$moduleInitializerString
       |}
       |""".stripMargin

  private def moduleInitializer(mainClassName: String): String =
    s"""moduleInitializers {
       |    main {
       |      className = '${ForClass.testPackage}.$mainClassName'
       |      mainMethodName = 'main'
       |      mainMethodHasArgs = true
       |    }
       |  }
       |""".stripMargin
