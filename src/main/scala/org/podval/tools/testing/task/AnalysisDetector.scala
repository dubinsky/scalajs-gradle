package org.podval.tools.testing.task

import org.podval.tools.testing.worker.TaskDefTest
import sbt.internal.inc.{Analysis, Relations}
import sbt.testing.{AnnotatedFingerprint, Fingerprint, Framework, SubclassFingerprint}
import xsbt.api.{Discovered, Discovery}
import xsbti.api.{ClassLike, Companions, Definition}
import xsbti.compile.FileAnalysisStore
import java.io.File

object AnalysisDetector:
  private final class Detector(
    val isAnnotation: Boolean,
    val name: String,
    val isModule: Boolean,
    val fingerprint: Fingerprint,
    val framework: Framework
  )

  def detectTests(
    loadedFrameworks: Seq[Framework],
    analysisFile: File
  ): Seq[TestClass] =
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
      .flatMap((companions: Companions) =>
        Seq(
          companions.classApi,
          companions.objectApi
        ) ++
          companions.classApi.structure.declared.toSeq ++
          companions.classApi.structure.inherited.toSeq ++
          companions.objectApi.structure.declared.toSeq ++
          companions.objectApi.structure.inherited.toSeq
      )
      .filter {
        case c: ClassLike => c.topLevel
        case _ => false
      }

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

    val all: Seq[TestClass] = for
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
    yield
      val className: String = definition.asInstanceOf[ClassLike].name
      TestClass(
        className = className,
        sourceFilePath = relations.definesClass(className).head.id,
        framework = detector.framework,
        fingerprint = detector.fingerprint
      )

    // TODO [discovery] call tasks() with all tests for a given framework (SuiteSelector, explicitly = false)

    all

