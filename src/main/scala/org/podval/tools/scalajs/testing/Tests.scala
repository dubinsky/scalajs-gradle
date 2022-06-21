package org.podval.tools.scalajs.testing

import org.gradle.api.logging.Logger
import org.opentorah.util.Collections
import org.scalajs.jsenv.{Input, JSEnv}
import org.scalajs.logging.Logger as JSLogger
import org.scalajs.testing.adapter.TestAdapter
import sbt.internal.inc.Analysis
import sbt.testing.{AnnotatedFingerprint, Fingerprint, Framework, SubclassFingerprint, SuiteSelector, TaskDef}
import xsbt.api.{Discovered, Discovery}
import xsbti.api.{AnalyzedClass, ClassLike, Companions, Definition}

// Note: based on org.scalajs.sbtplugin.ScalaJSPluginInternal
// Note: based on sbt.Tests from org.scala-sbt.actions
// Note: based on sbt.Defaults
// TODO report events closer to them happening?
// TODO even with one group, fold the summaries
// TODO use Gradle classes:
//   org.gradle.api.tasks.testing.Test
//   org.gradle.api.tasks.testing.TestResult
//   org.gradle.api.tasks.testing.logging.TestLogEvent
//   org.gradle.api.tasks.testing.report
//   org.gradle.api.tasks.testing.results

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
    listeners: Listeners
  ): Unit =
    val testAdapterConfig: TestAdapter.Config = TestAdapter.Config()
      .withLogger(jsLogger)

    val loadedFrameworks: Seq[Framework] = TestAdapter(
      jsEnv = jsEnv,
      input = Seq(input),
      config = testAdapterConfig
    )
      .loadFrameworks(frameworkNames = TestFramework.all.map(_.implClassNames.toList))
      .flatten

    val frameworkRunners: Seq[(Framework, Set[TestDefinition])] = framework2tests(
      detectors = getDetectors(loadedFrameworks).flatten,
      definitions = getDefinitions(analysis)
    ).toList

    val task: Task[Output] = if frameworkRunners.isEmpty then Task(Output.empty) else
      for
        _ <- Task(listeners.safeForeach(_.doInit()))
        // TODO group into framework suites?
        results: Map[String, SuiteResult] <- TestRunnable.toTask(frameworkRunners.flatMap((framework: Framework, tests: Set[TestDefinition]) =>
          TestRunner(framework, listeners).toRunnables(tests)
        ))
        output = Output.processResults(results)
        _ <- Task(listeners.safeForeach(_.doComplete(output.overall)))
      yield
        output

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

  private def getDetectors(frameworks: Seq[Framework]): Seq[Option[Detector]] =
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

  // TODO why is reflection used instead of the direct call?
  private def getFingerprints(framework: Framework): Seq[Fingerprint] =
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
      yield detector.framework -> TestDefinition(TaskDef(
        definition.name,
        detector.fingerprint,
        false,
        Array(new SuiteSelector)
      ))

    Collections.mapValues(result.groupBy(_._1))(_.map(_._2).toSet)

//  val output = Tests.foldTasks(groupTasks, config.parallel)
//
//  val summaries =
//    runners map {
//      case (tf, r) =>
//        Tests.Summary(frameworks(tf).name, r.done())
//    }
//  out.copy(summaries = summaries)
//
//  def foldTasks(results: Seq[Task[Output]], parallel: Boolean): Task[Output] =
//    if (results.isEmpty) {
//      task { Output(TestResult.Passed, Map.empty, Nil) }
//    } else if (parallel) {
//      reduced[Output](
//        results.toIndexedSeq, {
//          case (Output(v1, m1, _), Output(v2, m2, _)) =>
//            Output(
//              (if (severity(v1) < severity(v2)) v2 else v1): TestResult,
//              Map((m1.toSeq ++ m2.toSeq): _*),
//              Iterable.empty[Summary]
//            )
//        }
//      )
//    } else {
//      def sequence(tasks: List[Task[Output]], acc: List[Output]): Task[List[Output]] =
//        tasks match {
//          case Nil => task(acc.reverse)
//          case hd :: tl =>
//            hd flatMap { out =>
//              sequence(tl, out :: acc)
//            }
//        }
//      sequence(results.toList, List()) map { ress =>
//        val (rs, ms) = ress.unzip { e =>
//          (e.overall, e.events)
//        }
//        val m = ms reduce { (m1: Map[String, SuiteResult], m2: Map[String, SuiteResult]) =>
//          Map((m1.toSeq ++ m2.toSeq): _*)
//        }
//        Output(overall(rs), m, Iterable.empty)
//      }
//    }
//  def overall(results: Iterable[TestResult]): TestResult =
//    results.foldLeft(TestResult.Passed: TestResult) { (acc, result) =>
//      if (severity(acc) < severity(result)) result else acc
//    }
