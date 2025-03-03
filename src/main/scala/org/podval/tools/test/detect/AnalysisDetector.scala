package org.podval.tools.test.detect

import sbt.internal.inc.{Analysis, Relations}
import sbt.testing.{Fingerprint, Framework, SuiteSelector, TaskDef}
import xsbt.api.{Discovered, Discovery}
import xsbti.VirtualFileRef
import xsbti.api.{ClassLike, Companions, Definition}
import xsbti.compile.FileAnalysisStore
import java.io.File

// TODO what about tests that are not a part of this Gradle project but come from a dependency?
// TODO I may want to try replacing AnalysisDetector with reading the class files.
object AnalysisDetector:
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

    val detectors: Seq[Detector] = (
      for
        framework: Framework <- loadedFrameworks
        fingerprint: Fingerprint <- framework.fingerprints
      yield
        Detector.get(fingerprint, framework)
    ).flatten

    for
      case (definition: Definition, discovered: Discovered) <-
        Discovery(
          detectors.filter((detector: Detector) => !detector.isAnnotation).map(_.name).toSet,
          detectors.filter((detector: Detector) =>  detector.isAnnotation).map(_.name).toSet
        )(
          definitions
        )
      detector: Detector <- detectors.filter(_.is(discovered))
    yield
      val className: String = definition.asInstanceOf[ClassLike].name
      val sourceFile: VirtualFileRef = relations.definesClass(className).head
      // Note: is this how I get the class file path?!
      val dollar: String = if !discovered.isModule then "" else "$"
      val classFileName: String = className.replace('.', '/') + dollar + ".class"
      val classFilePath: String = relations.products(sourceFile).map(_.id).find(_.endsWith(classFileName)).get

      TestClass(
        sourceFilePath = sourceFile.id,
        classFilePath = classFilePath,
        framework = detector.framework,
        taskDef = TaskDef(
          className,
          detector.fingerprint,
          // defaults set here will be adjusted by TestClass.set() called from TestFrameworkDetector.processTestClass
          false,
          Array(new SuiteSelector)
        )
      )

