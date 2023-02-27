package org.podval.tools.testing

import org.gradle.api.Action
import org.gradle.api.internal.tasks.testing.junit.result.{TestClassResult, TestMethodResult, TestResultSerializer}
import org.gradle.api.tasks.testing.TestResult.ResultType
import org.gradle.testkit.runner.{BuildResult, GradleRunner}
import org.opentorah.util.Files
import java.io.File
import scala.jdk.CollectionConverters.*

object TestProject:
  private def root: File =
  // .../build/resources/test/org/podval/tools/test/anchor.txt
    Files.url2file(getClass.getResource("anchor.txt"))
      .getParentFile
      .getParentFile
      .getParentFile
      .getParentFile
      .getParentFile
      .getParentFile
      .getParentFile
      .getParentFile

  private def run(name: String): List[TestClassResult] =
    val projectDir: File = Files.file(root, "test-projects", name)
    val result: BuildResult = GradleRunner
      .create
      .withProjectDir(projectDir)
      .withArguments("clean", "test")
      .buildAndFail()

    //    println(s"--- OUTPUT OF THE TEST PROJECT $name ---")
    //    println(result.getOutput)
    //    println("--- ---")

    val binaryTestReportDir = Files.file(projectDir, "build", "test-results", "test", "binary")
    //    println(s"BINARY TEST REPORT DIR: $binaryTestReportDir")
    val testResults: TestResultSerializer = TestResultSerializer(binaryTestReportDir)

    var classResults: List[TestClassResult] = List.empty
    val visitor: Action[TestClassResult] = (result: TestClassResult) => classResults = result +: classResults
    testResults.read(visitor)
    classResults

  def forClass(
    className: String,
    failed: Int,
    skipped: Int,
    methods: MethodExpectation*
  ): ClassExpectation = new ClassExpectation(
    className,
    failed,
    skipped,
    methods
  )

  class ClassExpectation(
    val className: String,
    val failed: Int,
    val skipped: Int,
    val methods: Seq[MethodExpectation]
  )

  def passed (methodName: String): MethodExpectation = MethodExpectation(methodName, ResultType.SUCCESS)
  def failed (methodName: String): MethodExpectation = MethodExpectation(methodName, ResultType.FAILURE)
  def skipped(methodName: String): MethodExpectation = MethodExpectation(methodName, ResultType.SKIPPED)

  class MethodExpectation(
    val methodName: String,
    val resultType: ResultType
  )

  given CanEqual[ResultType, ResultType] = CanEqual.derived

  def forProject(
    project: String,
    classes: ClassExpectation*
  ): Unit =
    val results: List[TestClassResult] = run(project)

    def getForClass(name: String): TestClassResult = results.find(_.getClassName == name).get

    //    println("--- TEST RESULTS ---")
    //    for result: TestClassResult <- results do
    //      println(s"${result.getId}: ${result.getClassName} failed=${result.getFailuresCount} skipped=${result.getSkippedCount}")
    //      for result: TestMethodResult <- result.getResults.asScala do
    //        println(s"  ${result.getId}: ${result.getName} resultType=${result.getResultType}")
    //    println("--- ---")

    for classExpectation: ClassExpectation <- classes do
      val testClassResult: TestClassResult = getForClass(classExpectation.className)
      require(testClassResult.getFailuresCount == classExpectation.failed)
      require(testClassResult.getSkippedCount  == classExpectation.skipped)

      val testMethodResults: List[TestMethodResult] = testClassResult.getResults.asScala.toList
      for methodExpectation: MethodExpectation <- classExpectation.methods do
        val testMethodResult: TestMethodResult = testMethodResults.find(_.getName == methodExpectation.methodName).get
        require(testMethodResult.getResultType == methodExpectation.resultType)
