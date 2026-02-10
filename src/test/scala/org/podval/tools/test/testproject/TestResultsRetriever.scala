package org.podval.tools.test.testproject

import org.gradle.api.Action
import org.gradle.api.internal.tasks.testing.junit.result.{TestClassResult, TestMethodResult}
import org.gradle.api.internal.tasks.testing.report.generic.TestTreeModelResultsProvider
import org.podval.tools.build.Backend
import org.podval.tools.util.Files
import scala.jdk.CollectionConverters.ListHasAsScala
import java.io.File

final class TestResultsRetriever(projectDir: File):
  def testResults(backend: Option[Backend]): List[TestClassResult] =
    val binaryTestReportDir: File = Files.fileSeq(
      projectDir,
      backend.toSeq.map(_.sourceRoot) ++ Seq("build", "test-results", "test", "binary")
    )

    var classResults: List[TestClassResult] = List.empty

    TestTreeModelResultsProvider.useResultsFrom(
      binaryTestReportDir.toPath,
      _.visitClasses((testClassResult: TestClassResult) =>
        classResults = testClassResult +: classResults
      )
    )

    classResults

object TestResultsRetriever:
  private def dumpTestClassResults(results: List[TestClassResult]): List[String] = (
    for result: TestClassResult <- results yield
      val classSummary: String = s"${result.getId}: ${result.getClassName} failed=${result.getFailuresCount} skipped=${result.getSkippedCount}"
      val methodResults: List[String] = for result: TestMethodResult <- result.getResults.asScala.toList yield
        s"  ${result.getId}: ${result.getName} resultType=${result.getResultType}"
      List(classSummary) ++ methodResults
    ).flatten

//  def main(args: Array[String]): Unit =
//    dumpTestClassResults(
//      TestResultsRetriever(File(
////        "/home/dub/Podval/scalajs/cross-compile-example/core"
//        "/home/dub/Podval/scalajs/scalajs-gradle/build/test-projects/nested suites/combined/in Scala v3.8.0/mixed/nested suites-combined-in Scala v3.8.0-mixed"
////        "/home/dub/Podval/scalajs/test-projects/nested suites-combined-in Scala v3.8.0-mixed"
//      ))
//        .testResults(Some(org.podval.tools.scalajs.ScalaJSBackend))
//    ).foreach(println)
