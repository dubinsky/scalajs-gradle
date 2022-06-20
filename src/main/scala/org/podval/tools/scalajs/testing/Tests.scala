package org.podval.tools.scalajs.testing

import org.gradle.api.logging.Logger
import org.opentorah.util.Collections
import org.scalajs.jsenv.{Input, JSEnv}
import org.scalajs.logging.Logger as JSLogger
import org.scalajs.testing.adapter.TestAdapter
import sbt.internal.inc.Analysis
import sbt.testing.{AnnotatedFingerprint, Fingerprint, Framework, SubclassFingerprint, SuiteSelector}
import xsbt.api.{Discovered, Discovery}
import xsbti.api.{AnalyzedClass, ClassLike, Companions, Definition}

// Note: based on sbt.Tests from org.scala-sbt.actions
// Note: based on org.scalajs.sbtplugin.ScalaJSPluginInternal
// Note: based on sbt.Defaults
// TODO report events closer to them happening?
// TODO even with one group, fold the summaries
object Tests:

  private final class Detector(
    val isAnnotation: Boolean,
    val name: String,
    val isModule: Boolean,
    val fingerprint: Fingerprint,
    val framework: Framework
  ):
    def isDetected(discovered: Discovered): Boolean =
      (discovered.isModule == isModule) &&
      (if isAnnotation then discovered.annotations else discovered.baseClasses).contains(name)

  def run(
    jsEnv: JSEnv,
    input: Input,
    analysis: Analysis,
    jsLogger: JSLogger,
    logger: TestLogger,
    log: Logger
  ): Unit =
    val listeners: Listeners = Listeners(Seq(logger), log)

    val testAdapterConfig: TestAdapter.Config = TestAdapter.Config()
      .withLogger(jsLogger)

    val loadedFrameworks: Seq[Framework] = TestAdapter(
      jsEnv = jsEnv,
      input = Seq(input),
      config = testAdapterConfig
    )
      .loadFrameworks(frameworkNames = TestFramework.all.map(_.implClassNames.toList))
      .flatten

    val frameworkRuns: Seq[FrameworkRun] =
      for (framework: Framework, tests: Set[TestDefinition]) <-
        framework2tests(
          getDetectors(loadedFrameworks),
          getDefinitions(analysis)
        ).toList
      yield FrameworkRun(
        framework,
        tests,
        listeners
      )

    val task: Task[Output] =
      (if frameworkRuns.isEmpty then Task.noop else Task(listeners.safeForeach(_.doInit())))
        .flatMap(_ => TestRunnable.toTasks(frameworkRuns.flatMap(_.testTasks)))
        .map(Output.processResults)
        .flatMap((output: Output) =>
          (if frameworkRuns.isEmpty then Task.noop else Task(listeners.safeForeach(_.doComplete(output.overall))))
            .map(_ => output)
        )

    val output: Output = task.run()

    // getLogger.lifecycle(output.toString)

  private def getDefinitions(analysis: Analysis): Seq[Definition] =
    analysis.apis.internal.values.toSeq.flatMap { (ac: AnalyzedClass) =>
      val companions: Companions = ac.api
      Seq(
        companions.classApi: Definition,
        companions.objectApi: Definition
      ) ++
        companions.classApi .structure.declared .toSeq ++
        companions.classApi .structure.inherited.toSeq ++
        companions.objectApi.structure.declared .toSeq ++
        companions.objectApi.structure.inherited.toSeq
    }.filter {
      case c: ClassLike => c.topLevel
      case _            => false
    }

  private def getDetectors(frameworks: Seq[Framework]): Seq[Detector] =
    val detectorOpts: Seq[Option[Detector]] =
      for
        framework <- frameworks
        fingerprint <- getFingerprints(framework)
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

  private def getFingerprints(framework: Framework): Seq[Fingerprint] =
  // TODO why is reflection used instead of the direct call?
    framework.getClass.getMethod("fingerprints").invoke(framework) match
      case fingerprints: Array[Fingerprint] => fingerprints.toList
      case _                                => sys.error(s"Could not call 'fingerprints' on framework $framework")

  private def framework2tests(
    detectors: Seq[Detector],
    definitions: Seq[Definition]
  ): Map[Framework, Set[TestDefinition]] =
    val result: Seq[(Framework, TestDefinition)] =
      for
        (definition: Definition, discovered: Discovered) <-
          Discovery(
            detectors.filter(detector => !detector.isAnnotation).map(_.name).toSet,
            detectors.filter(detector =>  detector.isAnnotation).map(_.name).toSet
          )(definitions)
        detector: Detector <- detectors.filter(_.isDetected(discovered))
      // TODO: To pass in correct explicitlySpecified and selectors
      yield detector.framework -> new TestDefinition(
        name = definition.name,
        fingerprint = detector.fingerprint,
        explicitlySpecified = false,
        selectors = Array(new SuiteSelector)
      )

    Collections.mapValues(result.groupBy(_._1))(_.map(_._2).toSet)
