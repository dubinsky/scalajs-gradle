package org.podval.tools.test

import org.opentorah.util.Collections
import sbt.internal.inc.Analysis
import sbt.testing.{AnnotatedFingerprint, Fingerprint, Framework, SubclassFingerprint}
import xsbt.api.{Discovered, Discovery}
import xsbti.api.{ClassLike, Companions, Definition}
import xsbti.compile.FileAnalysisStore
import java.io.File

final class TestDiscovery(
  loadedFrameworks: Seq[Framework],
  analysisFile: File,
  testFiltering: TestFiltering
):

  import TestDiscovery.Detector

  private val detectors: Seq[Detector] =
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

  private def definitions: Seq[Definition] = FileAnalysisStore
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

  private val testsSeq: Seq[(Framework, TestDescriptor.Class)] =
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
      (includeClass: Boolean, includeMethods: Seq[String]) = testFiltering.whatToIncludeForClass(className)
      if includeClass || includeMethods.nonEmpty
    yield
      detector.framework -> TestDescriptor.Class(
        id = null,
        className = className,
        fingerprint = detector.fingerprint,
        explicitlySpecified = testFiltering.explicitlySpecified,
        includeMethods = includeMethods
      )

  val tests: Map[Framework, Seq[TestDescriptor.Class]] = Collections.mapValues(testsSeq.groupBy(_._1))(_.map(_._2))

object TestDiscovery:
  private final class Detector(
    val isAnnotation: Boolean,
    val name: String,
    val isModule: Boolean,
    val fingerprint: Fingerprint,
    val framework: Framework
  )
