package org.podval.tools.testing.detect

import org.gradle.api.internal.file.RelativeFile
import org.gradle.api.internal.tasks.testing.TestClassProcessor
import org.gradle.api.logging.{Logger, Logging}
import org.podval.tools.testing.{TaskDefTestSpec, TestEnvironment, TestInterface}
import sbt.testing.{Framework, Selector, SuiteSelector, TaskDef, TestSelector, TestWildcardSelector}
import scala.jdk.CollectionConverters.*
import java.io.File

// TODO rename
final class TestFrameworkDetector(
  testEnvironment: TestEnvironment,
  analysisFile: File,
  testFilter: TestFilter
) extends org.gradle.api.internal.tasks.testing.detection.TestFrameworkDetector:

  private val logger: Logger = Logging.getLogger(classOf[TestFrameworkDetector])

  def close(): Unit = testEnvironment.close()

  private var testClasses: Option[List[File]] = None
  override def setTestClasses(value: java.util.List[File]): Unit = testClasses = Some(value.asScala.toList)

  private var testClassPath: Option[List[File]] = None
  override def setTestClasspath(value: java.util.List[File]): Unit = testClassPath = Some(value.asScala.toList)

  private var testClassProcessor: Option[TestClassProcessor] = None
  private var loadedFrameworks: Option[List[Framework]] = None
  override def startDetection(value: TestClassProcessor): Unit =
    testClassProcessor = Some(value)
    val loadedFrameworks: List[Framework] = testEnvironment.loadFrameworks(testClassPath.get)
    // Check uniqueness; implementation class can not be used since in ScalaJS mode they all are
    // org.scalajs.testing.adapter.FrameworkAdapter
    require(loadedFrameworks.map(_.name).toSet.size == loadedFrameworks.size, "Different frameworks with the same name!")
    this.loadedFrameworks = Some(loadedFrameworks)

  private lazy val testClassesDetected: Seq[TestClass] = AnalysisDetector.detectTests(
    loadedFrameworks.get,
    analysisFile
  )

  // Note: called by org.gradle.api.internal.tasks.testing.detection.DefaultTestClassScanner
  override def processTestClass(relativeFile: RelativeFile): Boolean =
    val classFilePath: String = relativeFile.getFile.getAbsolutePath
    val testClass: Option[TestClass] = testClassesDetected
      .find(_.classFilePath == classFilePath)
      .flatMap(filter)

    testClass.foreach: (testClass: TestClass) =>
      val taskDefStr: String = TestInterface.toString(testClass.taskDef)
      logger.info(s"TestFrameworkDetector.processTestClass($taskDefStr)", null, null, null)
      testClassProcessor.get.processTestClass(TaskDefTestSpec(
        frameworkOrName = Right(testClass.framework),
        taskDef = testClass.taskDef
      ))

    testClass.isDefined

  private def filter(testClass: TestClass): Option[TestClass] =
    for matches: TestFilter.Matches <- testFilter.matchesClass(testClass.taskDef.fullyQualifiedName) yield
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

      TestClass(
        sourceFilePath = testClass.sourceFilePath,
        classFilePath = testClass.classFilePath,
        framework = testClass.framework,
        taskDef = TaskDef(
          testClass.taskDef.fullyQualifiedName,
          testClass.taskDef.fingerprint,
          explicitlySpecified,
          selectors
        )
      )

//    Note: I could run test classes through the Frameworks here like sbt does - but why?
//    for (frameworkName: String, tests: Seq[TestClass]) <- testClassesRaw.groupBy(_.framework.name) do
//      require(tests.nonEmpty)
//      val runner: Runner = TestClassProcessor.runner(tests.head.framework, testTagsFilter)
//      val taskDefs: Seq[TaskDef] = for testClass: TestClass <- tests yield testClass.taskDef
//      val tasks: Array[Task] = runner.tasks(taskDefs.toArray)
//      ???
//      runner.done()
