package org.podval.tools.test

import org.gradle.internal.id.{IdGenerator, LongIdGenerator}
import org.gradle.api.internal.tasks.testing.TestClassProcessor
import sbt.internal.inc.Analysis
import sbt.testing.{AnnotatedFingerprint, Fingerprint, Framework, Selector, SubclassFingerprint}
import xsbt.api.{Discovered, Discovery}
import xsbti.api.{ClassLike, Companions, Definition}
import xsbti.compile.FileAnalysisStore
import java.io.File

final class TestClassScanner(
  loadedFrameworks: Seq[Framework],
  analysisFile: File,
  includes: Set[String],
  excludes: Set[String],
  commandLineIncludes: Set[String],
  testClassProcessor: TestClassProcessor
) extends Runnable:

  import TestClassScanner.Detector

  private val testFiltering: TestFiltering = TestFiltering(
    includes = includes,
    excludes = excludes,
    commandLineIncludes = commandLineIncludes
  )

  override def run(): Unit =

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
      (definition: Definition, discovered: Discovered) <-
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
      (explicitlySpecified: Boolean, selectors: Array[Selector]) = testFiltering.whatToIncludeForClass(className)
      if selectors.nonEmpty
    do
      val framework: Framework = detector.framework
      val test: TestClass = TestClass(
        parentId = FrameworkTest.id(framework),
        id = idGenerator.generateId,
        framework = framework,
        className = className,
        fingerprint = detector.fingerprint,
        explicitlySpecified = explicitlySpecified,
        selectors = selectors
      )
      testClassProcessor.processTestClass(test)

object TestClassScanner:

  private final class Detector(
    val isAnnotation: Boolean,
    val name: String,
    val isModule: Boolean,
    val fingerprint: Fingerprint,
    val framework: Framework
  )
