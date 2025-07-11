package org.podval.tools.test.detect

import org.gradle.api.internal.file.RelativeFile
import org.gradle.api.internal.tasks.testing.TestClassProcessor
import org.gradle.api.internal.tasks.testing.detection.TestFrameworkDetector
import org.gradle.internal.Factory
import org.podval.tools.test.filter.{SuiteTestFilterMatch, TestFilter, TestFilterMatch, TestsTestFilterMatch}
import org.podval.tools.test.framework.{FrameworkDescriptor, FrameworkProvider, JUnit4ScalaJS, JUnit4ScalaNative}
import org.podval.tools.test.taskdef.TestClassRun
import org.slf4j.{Logger, LoggerFactory}
import sbt.testing.{AnnotatedFingerprint, Fingerprint, Framework, SubclassFingerprint, TaskDef}
import scala.jdk.CollectionConverters.ListHasAsScala
import java.io.File

// Inspired by org.gradle.api.internal.tasks.testing.detection.AbstractTestFrameworkDetector.
final class SbtTestFrameworkDetector(
  loadedFrameworks: (testClasspath: Iterable[File]) => List[Framework],
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

  private lazy val classFileFinder: ClassFileFinder =
    logger.debug(s"SbtTestFrameworkDetector.detectors: $detectors")
    ClassFileFinder(
      testClasspath = testClasspath.toList.flatten,
      testClassesDirectories = testClassesDirectories.toList.flatten,
      testTaskTemporaryDir = testTaskTemporaryDir
    )

  // Detector based on bootsrappers for JUnit4 on Scala.js and Scala Native.
  // Treat the presence of the bootstrapper for a class as a presence of the JUnit4 annotation on the class.
  private final class BootstrapperDetector(
    annotatedFingerprintDetector: AnnotatedFingerprintDetector,
    bootstrapperClassNameSuffix: String
  ):
    def detect(testClassVisitor: TestClassVisitor): Unit = for
      classNameInternal: String <- testClassVisitor.classNameInternal
      _: File <- classFileFinder.findClassFile(classNameInternal + bootstrapperClassNameSuffix)
    yield
      testClassVisitor.addDetector(annotatedFingerprintDetector)

  private def bootstrapperDetector(
    junit4: FrameworkDescriptor,
    bootstrapperClassNameSuffix: String
  ): Option[BootstrapperDetector] = annotatedDetectors
    .find(_.framework.name == junit4.name)
    .map(BootstrapperDetector(_, bootstrapperClassNameSuffix))

  private lazy val bootstrapperDetectors: Seq[BootstrapperDetector] = Seq(
    bootstrapperDetector(JUnit4ScalaJS    , "$scalajs$junit$bootstrapper$"    ),
    bootstrapperDetector(JUnit4ScalaNative, "$scalanative$junit$bootstrapper$")
  ).flatten
  
  private def filter(
    className: String,
    detectors: FingerprintDetector.Many
  ): Option[TestClassRun] =
    if detectors.isEmpty then None else
      if detectors.size > 1 then
        logger.warn(s"SbtTestFrameworkDetector: Ignoring class $className with multiple detectors: $detectors.")
        None
      else
        val detector: FingerprintDetector = detectors.head
        testFilter.matchClass(className).map: (testFilterMatch: TestFilterMatch) =>
          val (testNames: Array[String], testWildcards: Array[String]) = testFilterMatch match
            case _: SuiteTestFilterMatch => (Array.empty[String], Array.empty[String])
            case testsTestFilterMatch: TestsTestFilterMatch => (
              testsTestFilterMatch.testNames.toArray,
              testsTestFilterMatch.testWildcards.toArray
            )

          TestClassRun(
            frameworkProvider = FrameworkProvider(detector.framework),
            getTestClassName = className,
            fingerprint = detector.fingerprint,
            explicitlySpecified = testFilterMatch.explicitlySpecified,
            testNames = testNames,
            testWildcards = testWildcards
          )
  
  // Called by org.gradle.api.internal.tasks.testing.detection.DefaultTestClassScanner.
  override def processTestClass(relativeFile: RelativeFile): Boolean =
    val testClassRun: Option[TestClassRun] =
      val testClassVisitor: TestClassVisitor = processClassFile(relativeFile.getFile)
      testClassVisitor.className.flatMap((className: String) => filter(
        className = className,
        detectors = testClassVisitor.getApplicableDetectors
      ))

    testClassRun.foreach(testClassProcessor.get.processTestClass)
    testClassRun.isDefined
  
  private def processClassFile(classFile: File): TestClassVisitor =
    val testClassVisitor: TestClassVisitor = TestClassVisitor(this, classFile)

    bootstrapperDetectors.foreach(_.detect(testClassVisitor))

    testClassVisitor.appendDetectors(testClassVisitor
      .getSuperTypes
      .flatMap(processSuperType)
      .foldLeft(ClassFileDetectors.empty)(_.add(_))
    )
    
    testClassVisitor
  
  private var superTypes: Map[File, ClassFileDetectors] = Map.empty

  private def processSuperType(superType: String): Option[ClassFileDetectors] =
    if SbtTestFrameworkDetector.hierarchyRoots.contains(superType) then None else classFileFinder
      .findClassFile(superType)
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
