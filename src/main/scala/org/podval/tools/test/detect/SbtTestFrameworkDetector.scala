package org.podval.tools.test.detect

import org.gradle.api.internal.file.RelativeFile
import org.gradle.api.internal.tasks.testing.TestClassProcessor
import org.gradle.api.internal.tasks.testing.detection.{ClassFileExtractionManager, TestFrameworkDetector}
import org.gradle.internal.Factory
import org.podval.tools.test.filter.{TestFilter, TestFilterMatch}
import org.podval.tools.test.framework.JUnit4ScalaJS
import org.podval.tools.test.taskdef.TestClassRunNonForking
import org.slf4j.{Logger, LoggerFactory}
import sbt.testing.{AnnotatedFingerprint, Fingerprint, Framework, SubclassFingerprint, TaskDef}
import scala.jdk.CollectionConverters.*
import java.io.File

// Inspired by org.gradle.api.internal.tasks.testing.detection.AbstractTestFrameworkDetector.
final class SbtTestFrameworkDetector(
  isScalaJS: Boolean,
  loadedFrameworks: (testClassPath: Iterable[File]) => List[Framework],
  testFilter: TestFilter,
  testTaskTemporaryDir: Factory[File]
) extends TestFrameworkDetector:

  import SbtTestFrameworkDetector.logger
  
  private var testClassesDirectories: Option[List[File]] = None
  override def setTestClasses(value: java.util.List[File]): Unit = testClassesDirectories = Some(value.asScala.toList)

  private var testClasspath: Option[List[File]] = None
  override def setTestClasspath(value: java.util.List[File]): Unit = testClasspath = Some(value.asScala.toList)

  private var testClassProcessor: Option[TestClassProcessor] = None
  override def startDetection(value: TestClassProcessor): Unit = testClassProcessor = Some(value)

  private lazy val detectors: Seq[FingerprintDetector] =
    for
      framework: Framework <- loadedFrameworks(testClasspath.get)
      fingerprint: Fingerprint <- framework.fingerprints
    yield fingerprint match
      case subclassFingerprint : SubclassFingerprint  => SubclassFingerprintDetector (subclassFingerprint , framework)
      case annotatedFingerprint: AnnotatedFingerprint => AnnotatedFingerprintDetector(annotatedFingerprint, framework)

  lazy val annotatedDetectors: Seq[AnnotatedFingerprintDetector] = detectors
    .filter(_.isInstanceOf[AnnotatedFingerprintDetector])
    .map   (_.asInstanceOf[AnnotatedFingerprintDetector])

  lazy val subclassDetectors : Seq[SubclassFingerprintDetector] = detectors
    .filter(_.isInstanceOf[SubclassFingerprintDetector])
    .map   (_.asInstanceOf[SubclassFingerprintDetector])

  private def filter(
    className: String,
    detectors: FingerprintDetectors
  ): Option[TestClassRunNonForking] =
    if detectors.isEmpty then None else
      if detectors.size > 1 then
        logger.warn(s"SbtTestFrameworkDetector: Ignoring class $className with multiple detectors: $detectors.")
        None
      else
        val detector: FingerprintDetector = detectors.head
        testFilter.matchClass(className).map: (testFilterMatch: TestFilterMatch) =>
          TestClassRunNonForking(
            framework = detector.framework,
            taskDef = TaskDef(
              className,
              detector.fingerprint,
              testFilterMatch.explicitlySpecified,
              testFilterMatch.selectors
            )
          )
  
  // Called by org.gradle.api.internal.tasks.testing.detection.DefaultTestClassScanner.
  override def processTestClass(relativeFile: RelativeFile): Boolean =
    val testClassRun: Option[TestClassRunNonForking] =
      val testClassVisitor: TestClassVisitor = processClassFile(relativeFile.getFile)
      testClassVisitor.className.flatMap((className: String) => filter(
        className = className,
        detectors = testClassVisitor.getApplicableDetectors
      ))

    testClassRun.foreach(testClassProcessor.get.processTestClass)
    testClassRun.isDefined

  // --- class-file detection

  private def testClasspathFiles: List[File] = testClasspath.toList.flatten

  private lazy val testClassDirectories: List[File] =
    testClassesDirectories.toList.flatten ++ testClasspathFiles.filter(_.isDirectory)

  private lazy val classFileExtractionManager: ClassFileExtractionManager =
    logger.info(s"SbtTestFrameworkDetector.testClasspath: $testClasspathFiles")
    logger.info(s"SbtTestFrameworkDetector.detectors: $detectors")
    val result: ClassFileExtractionManager = ClassFileExtractionManager(testTaskTemporaryDir)
    testClasspathFiles
      .filter((file: File) => file.isFile && file.getPath.endsWith(".jar"))
      .foreach((libraryJar: File) =>
        logger.info(s"SbtTestFrameworkDetector: libraryJar $libraryJar")
        result.addLibraryJar(libraryJar)
      )
    result

  // JUni4 for Scala.js annotated detector if running on Scala.js with JUnit for for Scala.js on classpath.
  private lazy val jUnit4ScalaJSAnnotatedDetector: Option[AnnotatedFingerprintDetector] =
    if !isScalaJS then None else annotatedDetectors.find(_.framework.name == JUnit4ScalaJS.name)

  private def processClassFile(classFile: File): TestClassVisitor =
    val testClassVisitor: TestClassVisitor = TestClassVisitor(this, classFile)

    // Treat the presence of the bootstrapper for a class as a presence of the JUnit4 annotation on the class.
    for
      jUnit4ScalaJSAnnotatedDetector: AnnotatedFingerprintDetector <- jUnit4ScalaJSAnnotatedDetector
      classNameInternal: String <- testClassVisitor.classNameInternal
      _: File <- findClassFile(classNameInternal + "$scalajs$junit$bootstrapper$")
    yield
      testClassVisitor.addDetector(jUnit4ScalaJSAnnotatedDetector)

    testClassVisitor.appendDetectors(testClassVisitor
      .getSuperTypes
      .flatMap(processSuperType)
      .foldLeft(ClassFileDetectors.empty)(_.add(_))
    )
    
    testClassVisitor
  
  private def findClassFile(typeName: String): Option[File] = testClassDirectories
    .map((testClassDirectory: File) => File(testClassDirectory, s"$typeName.class"))
    .find(_.exists)
    .orElse(Option(classFileExtractionManager.getLibraryClassFile(typeName)))
    .orElse:
      logger.info(s"SbtTestFrameworkDetector: could not find $typeName file to scan.")
      None

  private var superTypes: Map[File, ClassFileDetectors] = Map.empty

  private def processSuperType(superType: String): Option[ClassFileDetectors] =
    if SbtTestFrameworkDetector.hierarchyRoots.contains(superType) then None else findClassFile(superType)
      .map((superTypeFile: File) => superTypes.getOrElse(superTypeFile, {
        val detectors: ClassFileDetectors = processClassFile(superTypeFile).getDetectors
        superTypes = superTypes.updated(superTypeFile, detectors)
        detectors
      }))

object SbtTestFrameworkDetector:
  private val logger: Logger = LoggerFactory.getLogger(classOf[SbtTestFrameworkDetector])

  // type hierarchy roots are not tests
  private val hierarchyRoots: Set[String] = Set(
    "scala/AnyRef",
    "java/lang/Object",
  )
