package org.podval.tools.test.testproject

import org.gradle.api.Action
import org.gradle.api.internal.tasks.testing.junit.result.{TestClassResult, TestMethodResult, TestResultSerializer}
import org.podval.tools.build.ScalaBackend
import org.podval.tools.util.Files
import scala.jdk.CollectionConverters.ListHasAsScala
import java.io.File

final class TestResultsRetriever(projectDir: File):
  def testResults(backend: Option[ScalaBackend]): List[TestClassResult] =
    readTestClassResults(Files.fileSeq(
      projectDir,
      backend.toSeq.map(_.sourceRoot) ++ Seq("build", "test-results", "test", "binary")
    ))

  private def readTestClassResults(binaryTestReportDir: File): List[TestClassResult] =
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
  