package org.podval.tools.testing.task

import org.gradle.api.internal.file.RelativeFile
import org.gradle.api.internal.tasks.testing.TestClassProcessor
import org.podval.tools.testing.worker.TaskDefTest
import sbt.testing.{Selector, SuiteSelector, TaskDef, TestSelector, TestWildcardSelector}
import java.io.File
import scala.jdk.CollectionConverters.*

// TODO [detection]
// - look at test discovery in ScalaJS and sbt
// - integrate with AbstractTestFrameworkDetector
// - deal with double-discovery and NestedXXXSelector
final class TestFrameworkDetector(
  analysisTestClasses: Seq[TestClass],
  testFilter: TestFilter
) extends org.gradle.api.internal.tasks.testing.detection.TestFrameworkDetector:

  override def setTestClasses(testClasses: java.util.List[File]): Unit =
//    println(testClasses.asScala.mkString("----- TestFrameworkDetector.setTestClasses", "\n", "\n-----"))
    ()
  override def setTestClasspath(classpath: java.util.List[File]): Unit =
//    println(classpath.asScala.mkString("----- TestFrameworkDetector.setTestClasspath", "\n", "\n-----"))
    ()

  private var testClassProcessor: Option[TestClassProcessor] = None
  override def startDetection(testClassProcessor: TestClassProcessor): Unit =
    this.testClassProcessor = Some(testClassProcessor)

  private var done: Boolean = false
  override def processTestClass(testClassFile: RelativeFile): Boolean =
    if !done then run()
    done = true
    true

  private def run(): Unit =
    for
      testClass: TestClass <- analysisTestClasses
      case matches: TestFilter.Matches <- testFilter.matchesClass(testClass.className)
    do
      val explicitlySpecified: Boolean = matches match
        case TestFilter.Matches.Suite(explicitlySpecified) => explicitlySpecified
        case _ => true

      val selectors: Array[Selector] = matches match
        case TestFilter.Matches.Suite(_) =>
          Array(new SuiteSelector)
        case TestFilter.Matches.Tests(testNames, testWildCards) =>
          testNames.toArray.map(TestSelector(_)) ++ testWildCards.toArray.map(TestWildcardSelector(_))

      require(!matches.isEmpty)
      require(selectors.nonEmpty)

      val test: TaskDefTest = TaskDefTest(
        id = null,
        framework = Right(testClass.framework),
        taskDef = TaskDef(
          testClass.className,
          testClass.fingerprint,
          explicitlySpecified,
          selectors
        )
      )

      testClassProcessor.get.processTestClass(test)
