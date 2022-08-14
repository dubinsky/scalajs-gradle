package org.podval.tools.test

import org.gradle.api.internal.tasks.testing.TestClassProcessor
import org.gradle.internal.id.{IdGenerator, LongIdGenerator}
import sbt.internal.inc.Analysis
import sbt.testing.{AnnotatedFingerprint, Fingerprint, Framework, Selector, SubclassFingerprint, SuiteSelector, TaskDef,
  TestSelector, TestWildcardSelector}
import xsbt.api.{Discovered, Discovery}
import xsbti.api.{ClassLike, Companions, Definition}
import xsbti.compile.FileAnalysisStore
import java.io.File

object TestScanner:
  private final class Detector(
    val isAnnotation: Boolean,
    val name: String,
    val isModule: Boolean,
    val fingerprint: Fingerprint,
    val framework: Framework
  )

  def run(
    groupByFramework: Boolean,
    loadedFrameworks: Seq[Framework],
    analysisFile: File,
    testFilter: TestFilter,
    testClassProcessor: TestClassProcessor
  ): Unit =

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

    val idGenerator: IdGenerator[?] = new LongIdGenerator

    for
      case (definition: Definition, discovered: Discovered) <-
        Discovery(
          detectors.filter(detector => !detector.isAnnotation).map(_.name).toSet,
          detectors.filter(detector =>  detector.isAnnotation).map(_.name).toSet
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
        getParentId = RootTest.forFramework(framework, groupByFramework).getId,
        getId = idGenerator.generateId,
        framework = framework,
        taskDef = TaskDef(
          className,
          detector.fingerprint,
          explicitlySpecified,
          selectors
        )
      )
      testClassProcessor.processTestClass(test)
