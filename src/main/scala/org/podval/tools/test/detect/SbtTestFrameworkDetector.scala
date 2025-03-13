package org.podval.tools.test.detect

import org.gradle.api.internal.file.RelativeFile
import org.gradle.api.internal.tasks.testing.TestClassProcessor
import org.gradle.api.internal.tasks.testing.detection.{ClassFileExtractionManager, TestFrameworkDetector}
import org.gradle.internal.Factory
import org.podval.tools.test.filter.{TestFilter, TestFilterMatch}
import org.podval.tools.test.framework.JUnit4ScalaJS
import org.podval.tools.test.taskdef.TestClassRunNonForking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import sbt.internal.inc.{Analysis, Relations}
import sbt.testing.{AnnotatedFingerprint, Fingerprint, Framework, SubclassFingerprint, TaskDef}
import xsbt.api.{Discovered, Discovery}
import xsbti.VirtualFileRef
import xsbti.api.{ClassLike, Companions, Definition}
import xsbti.compile.FileAnalysisStore
import scala.jdk.CollectionConverters.*
import java.io.File

// Inspired by org.gradle.api.internal.tasks.testing.detection.AbstractTestFrameworkDetector.
final class SbtTestFrameworkDetector(
  isScalaJS: Boolean,
  loadedFrameworks: (testClassPath: Iterable[File]) => List[Framework],
  testFilter: TestFilter,
  testTaskTemporaryDir: Factory[File],
  useSbtAnalysisTestDetection: Boolean,
  analysisFile: File
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
      if useSbtAnalysisTestDetection
      then
        testClassesDetectedFromAnalysis.get(relativeFile.getFile.getAbsolutePath)
      else
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

  // --- analysis detection

  private lazy val testClassesDetectedFromAnalysis: Map[String, TestClassRunNonForking] = detectFromAnalysis.flatten.toMap

  private def detectFromAnalysis: Seq[Option[(String, TestClassRunNonForking)]] =
    val analysis: Analysis = FileAnalysisStore
      .getDefault(analysisFile)
      .get
      .get
      .getAnalysis
      .asInstanceOf[Analysis]

    val relations: Relations = analysis.relations

    val definitions: Seq[Definition] = analysis
      .apis
      .internal
      .values
      .toSeq
      .map(_.api)
      .flatMap: (companions: Companions) =>
        Seq(
          companions.classApi,
          companions.objectApi
        ) ++
          companions.classApi .structure.declared .toSeq ++
          companions.classApi .structure.inherited.toSeq ++
          companions.objectApi.structure.declared .toSeq ++
          companions.objectApi.structure.inherited.toSeq
      .filter:
        case c: ClassLike => c.topLevel
        case _ => false

    for
      case (definition: Definition, discovered: Discovered) <-
        Discovery(
          subclasses  = subclassDetectors .map(_.name).toSet,
          annotations = annotatedDetectors.map(_.name).toSet
        )(
          definitions
        )
    yield
      val className: String = definition.asInstanceOf[ClassLike].name
      filter(
        className, 
        detectors.toSet.filter(_.isDiscovered(discovered))
      ).map: (testClassRun: TestClassRunNonForking) =>
        // Is this really the way to get the class file path?!
        val sourceFile: VirtualFileRef = relations.definesClass(className).head
        val dollar: String = if !discovered.isModule then "" else "$"
        val classFileName: String = className.replace('.', '/') + dollar + ".class"
        val classFilePath: String = relations.products(sourceFile).map(_.id).find(_.endsWith(classFileName)).get
        classFilePath -> testClassRun

object SbtTestFrameworkDetector:
  private val logger: Logger = LoggerFactory.getLogger(classOf[SbtTestFrameworkDetector])

  // type hierarchy roots are not tests
  private val hierarchyRoots: Set[String] = Set(
    "scala/AnyRef",
    "java/lang/Object",
  )
