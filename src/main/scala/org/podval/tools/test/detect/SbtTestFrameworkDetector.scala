package org.podval.tools.test.detect

import org.gradle.api.internal.file.RelativeFile
import org.gradle.api.internal.tasks.testing.TestClassProcessor
import org.gradle.api.internal.tasks.testing.detection.{ClassFileExtractionManager, TestFrameworkDetector}
import org.gradle.internal.Factory
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Type
import org.podval.tools.test.environment.TestEnvironment
import org.podval.tools.test.filter.{TestFilter, TestFilterMatch}
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
import scala.util.Using
import java.io.{BufferedInputStream, File, FileInputStream}

// Note: inspired by org.gradle.api.internal.tasks.testing.detection.AbstractTestFrameworkDetector.
// Note: SBT Scala compiler analysis-based test detection mechanism is unlikely to detect tests whose sources are not in the project. 
// Detection mechanism based on class-file canning has been implemented and is now the default - unless running on Scala.js,
// where annotations are not delivered by the `org.objectweb.asm.ClassReader`, because they are not there:
// `Scala.js` compiler does not insert `RuntimeVisibleAnnotations` into the class-file.
final class SbtTestFrameworkDetector(
  testEnvironment: TestEnvironment,
  testFilter: TestFilter,
  testTaskTemporaryDir: Factory[File],
  useSbtAnalysisTestDetection: Boolean,
  analysisFile: File
) extends TestFrameworkDetector:
  private val logger: Logger = LoggerFactory.getLogger(classOf[SbtTestFrameworkDetector])

  private var testClassesDirectories: Option[List[File]] = None
  override def setTestClasses(value: java.util.List[File]): Unit = testClassesDirectories = Some(value.asScala.toList)

  private var testClasspath: Option[List[File]] = None
  override def setTestClasspath(value: java.util.List[File]): Unit = testClasspath = Some(value.asScala.toList)
  private def testClasspathFiles: List[File] = testClasspath.toList.flatten

  private var testClassProcessor: Option[TestClassProcessor] = None
  override def startDetection(value: TestClassProcessor): Unit = testClassProcessor = Some(value)

  private lazy val detectors: Seq[FingerprintDetector] =
    for
      framework: Framework <- testEnvironment.loadedFrameworks(testClasspath.get)
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
    detectors: Set[FingerprintDetector]
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

  // Note: called by org.gradle.api.internal.tasks.testing.detection.DefaultTestClassScanner
  override def processTestClass(relativeFile: RelativeFile): Boolean =
    val testClassRun: Option[TestClassRunNonForking] =
      if useSbtAnalysisTestDetection
      then
        testClassesDetectedFromAnalysis.get(relativeFile.getFile.getAbsolutePath)
      else
        val testClassVisitor: TestClassVisitor = processClassFile(relativeFile.getFile)
        testClassVisitor.className.flatMap((className: String) => filter(
          className = Type.getObjectType(className).getClassName,
          detectors = testClassVisitor.getApplicableDetectors
        ))

    testClassRun.foreach(testClassProcessor.get.processTestClass)
    testClassRun.isDefined

  // --- analysis detection

  private lazy val testClassesDetectedFromAnalysis: Map[String, TestClassRunNonForking] = detectTests.flatten.toMap

  private def detectTests: Seq[Option[(String, TestClassRunNonForking)]] =
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
      filter(className, detectors.toSet.filter(_.isDiscovered(discovered))).map: (testClassRun: TestClassRunNonForking) =>
        // Note: is this how I get the class file path?!
        val sourceFile: VirtualFileRef = relations.definesClass(className).head
        val dollar: String = if !discovered.isModule then "" else "$"
        val classFileName: String = className.replace('.', '/') + dollar + ".class"
        val classFilePath: String = relations.products(sourceFile).map(_.id).find(_.endsWith(classFileName)).get
        classFilePath -> testClassRun

  // --- class-file detection

  private lazy val testClassDirectories: List[File] =
    testClassesDirectories.toList.flatten ++ testClasspathFiles.filter(_.isDirectory)

  private lazy val classFileExtractionManager: ClassFileExtractionManager =
    logger.info(s"SbtTestFrameworkDetector.testClasspath: $testClasspathFiles")
    val result: ClassFileExtractionManager = ClassFileExtractionManager(testTaskTemporaryDir)
    testClasspathFiles
      .filter((file: File) => file.isFile && file.getPath.endsWith(".jar"))
      .foreach((libraryJar: File) =>
        logger.info(s"SbtTestFrameworkDetector: adding libraryJar $libraryJar")
        result.addLibraryJar(libraryJar)
      )
    logger.info(s"SbtTestFrameworkDetector detectors: $detectors")
    result

  private var superTypes: Map[File, ClassFileDetectors] = Map.empty

  private def processClassFile(classFile: File): TestClassVisitor =
    val testClassVisitor: TestClassVisitor = TestClassVisitor(this)

    Using(BufferedInputStream(FileInputStream(classFile)))((classStream: BufferedInputStream) =>
      ClassReader(classStream.readAllBytes)
        .accept(testClassVisitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES)
    ).recover((e: Throwable) => 
      logger.debug(s"Failed to read class file ${classFile.getAbsolutePath}; assuming it's not a test class and continuing", e)
    )

    testClassVisitor.getSuperTypes.foreach((superType: String) => processSuperType(testClassVisitor, superType))
    testClassVisitor
  
  private def processSuperType(
    testClassVisitor: TestClassVisitor,
    superType: String
  ): Unit = testClassDirectories
    .map((testClassDirectory: File) => File(testClassDirectory, s"$superType.class"))
    .find(_.exists)
    .orElse(
      if SbtTestFrameworkDetector.hierarchyRoots.contains(superType) then None else
        // superClass file not in test class directories; look inside JARs
        Option(classFileExtractionManager.getLibraryClassFile(superType))
    ) match
      case None =>
        logger.info(s"Could not find supertype $superType to scan")
      case Some(superTypeFile) =>
        testClassVisitor.addDetectors(superTypes.get(superTypeFile) match
          case Some(detectors) =>
            detectors
          case None =>
            val detectors: ClassFileDetectors = processClassFile(superTypeFile).getDetectors
            superTypes = superTypes.updated(superTypeFile, detectors)
            detectors
        )

object SbtTestFrameworkDetector:
  // type hierarchy roots are not test cases
  private val hierarchyRoots: Set[String] = Set(
    "java/lang/Object"
  )
  