package org.podval.tools.test

import org.gradle.api.internal.file.RelativeFile
import org.gradle.api.internal.tasks.testing.TestClassProcessor
import org.gradle.internal.id.{CompositeIdGenerator, IdGenerator, LongIdGenerator}
import org.opentorah.build.Gradle
import sbt.internal.inc.Analysis
import sbt.testing.{AnnotatedFingerprint, Fingerprint, Framework, Selector, SubclassFingerprint, SuiteSelector, TaskDef,
  TestSelector, TestWildcardSelector}
import xsbt.api.{Discovered, Discovery}
import xsbti.api.{ClassLike, Companions, Definition}
import xsbti.compile.FileAnalysisStore
import java.io.File


object TestFrameworkDetector:
  private final class Detector(
    val isAnnotation: Boolean,
    val name: String,
    val isModule: Boolean,
    val fingerprint: Fingerprint,
    val framework: Framework
  )

import TestFrameworkDetector.Detector

// TODO instead of running all the files, look at the AnalyzedClass.provenance;
// since then the analyzis data sticks around, make the detector a var in TestFramework - and make sure to release it on close()!
final class TestFrameworkDetector(
  filesToAddToClassPath: Iterable[File],
  loadedFrameworks: Seq[Framework],
  analysisFile: File,
  testFilter: TestFilter
) extends org.gradle.api.internal.tasks.testing.detection.TestFrameworkDetector:

  override def setTestClasses(testClasses: java.util.List[File]): Unit = ()
  override def setTestClasspath(classpath: java.util.List[File]): Unit = ()

  private var testClassProcessor: Option[TestClassProcessor] = None
  override def startDetection(testClassProcessor: TestClassProcessor): Unit =
    this.testClassProcessor = Some(testClassProcessor)

  private var done: Boolean = false
  override def processTestClass(testClassFile: RelativeFile): Boolean =
    if !done then run()
    done = true
    true

  private def run(): Unit =
    // TODO classpath comment on the need - if needed ;)
    Gradle.addToClassPath(this, filesToAddToClassPath)

    val detectors: Seq[Detector] =
      val detectorOpts: Seq[Option[Detector]] = for
        framework: Framework <- loadedFrameworks
        fingerprint: Fingerprint <- framework.fingerprints
      yield fingerprint match
        case sub: SubclassFingerprint => Some(Detector(
          isAnnotation = false,
          name = sub.superclassName,
          isModule = sub.isModule,
          fingerprint = sub,
          framework = framework
        ))
        case ann: AnnotatedFingerprint => Some(Detector(
          isAnnotation = true,
          name = ann.annotationName,
          isModule = ann.isModule,
          fingerprint = ann,
          framework = framework
        ))
        case _ => None

      detectorOpts.flatten

    val definitions: Seq[Definition] = FileAnalysisStore
      .getDefault(analysisFile)
      .get
      .get
      .getAnalysis
      .asInstanceOf[Analysis]
      .apis
      .internal
      .values
      .toSeq
      .map(_.api)
      .flatMap((companions: Companions) =>
        Seq(
          companions.classApi ,
          companions.objectApi
        ) ++
          companions.classApi .structure.declared .toSeq ++
          companions.classApi .structure.inherited.toSeq ++
          companions.objectApi.structure.declared .toSeq ++
          companions.objectApi.structure.inherited.toSeq
      )
      .filter {
        case c: ClassLike => c.topLevel
        case _            => false
      }

    val idGenerator: IdGenerator[?] = CompositeIdGenerator(0L, new LongIdGenerator)

    for
      case (definition: Definition, discovered: Discovered) <-
        Discovery(
          detectors.filter((detector: Detector) => !detector.isAnnotation).map(_.name).toSet,
          detectors.filter((detector: Detector) =>  detector.isAnnotation).map(_.name).toSet
        )(
          definitions
        )
      detector: Detector <-
        detectors.filter((detector: Detector) =>
          (discovered.isModule == detector.isModule) &&
          (if detector.isAnnotation then discovered.annotations else discovered.baseClasses).contains(detector.name)
        )
      className: String = definition.asInstanceOf[ClassLike].name
      case matches: TestFilter.Matches <- testFilter.matchesClass(className)
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

      val framework: Framework = detector.framework
      val test: TaskDefTest = TaskDefTest(
        id = idGenerator.generateId,
        framework = framework,
        taskDef = TaskDef(
          className,
          detector.fingerprint,
          explicitlySpecified,
          selectors
        )
      )

      testClassProcessor.get.processTestClass(test)
