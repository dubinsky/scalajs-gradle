package org.podval.tools.test.detect

import org.gradle.api.internal.file.RelativeFile
import org.gradle.api.internal.tasks.testing.TestClassProcessor
import org.podval.tools.test.TestEnvironment
import org.podval.tools.test.taskdef.TestClassRunNonForking
import sbt.testing.Framework
import scala.jdk.CollectionConverters.ListHasAsScala
import java.io.File

final class TestFrameworkDetector(
  testEnvironment: TestEnvironment,
  analysisFile: File,
  testFilter: TestFilter
) extends org.gradle.api.internal.tasks.testing.detection.TestFrameworkDetector:
  
  def close(): Unit = testEnvironment.close()

  private var testClasses: Option[List[File]] = None
  override def setTestClasses(value: java.util.List[File]): Unit = testClasses = Some(value.asScala.toList)

  private var testClassPath: Option[List[File]] = None
  override def setTestClasspath(value: java.util.List[File]): Unit = testClassPath = Some(value.asScala.toList)

  private var testClassProcessor: Option[TestClassProcessor] = None
  override def startDetection(value: TestClassProcessor): Unit = testClassProcessor = Some(value)

  private lazy val testClassesDetected: Seq[TestClass] =
    val loadedFrameworks: List[Framework] = testEnvironment.loadFrameworks(testClassPath.get)
    
    // Check uniqueness; implementation class can not be used since in Scala.js mode they all are
    // org.scalajs.testing.adapter.FrameworkAdapter
    require(loadedFrameworks.map(_.name).toSet.size == loadedFrameworks.size, "Different frameworks with the same name!")
    
    AnalysisDetector.detectTests(
      loadedFrameworks,
      analysisFile
    )

  // Note: called by org.gradle.api.internal.tasks.testing.detection.DefaultTestClassScanner
  override def processTestClass(relativeFile: RelativeFile): Boolean =
    val classFilePath: String = relativeFile.getFile.getAbsolutePath
    val testClass: Option[TestClass] = testClassesDetected
      .find(_.classFilePath == classFilePath)
      .flatMap((testClass: TestClass) => testFilter
        .matchClass(testClass.taskDef.fullyQualifiedName)
        .map(testClass.set)
      )

    // Note: I could run test classes through the Frameworks here like sbt does - but why?
    testClass.foreach: (testClass: TestClass) =>
      testClassProcessor.get.processTestClass(TestClassRunNonForking(
        framework = testClass.framework,
        taskDef = testClass.taskDef
      ))

    testClass.isDefined
