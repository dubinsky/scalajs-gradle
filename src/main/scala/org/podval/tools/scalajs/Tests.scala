package org.podval.tools.scalajs

import org.gradle.api.internal.tasks.testing.results.DefaultTestResult
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.testing.TestResult
import org.gradle.api.tasks.testing.TestResult.ResultType
import org.opentorah.util.Collections
import org.scalajs.jsenv.{Input, JSEnv}
import org.scalajs.logging.Logger as JSLogger
import org.scalajs.testing.adapter.TestAdapter
import sbt.internal.inc.Analysis
import sbt.testing.{AnnotatedFingerprint, Fingerprint, Framework, SubclassFingerprint, SuiteSelector, TaskDef,
  Status as TStatus}
import xsbt.api.{Discovered, Discovery}
import xsbti.api.{AnalyzedClass, ClassLike, Companions, Definition}
import xsbti.compile.FileAnalysisStore
import java.io.File
import scala.jdk.CollectionConverters.*

// Note: based on org.scalajs.sbtplugin.ScalaJSPluginInternal
// Note: based on sbt.Tests from org.scala-sbt.actions
// Note: based on sbt.Defaults
// TODO use Gradle test selectors
// TODO use Gradle classes:
//   org.gradle.api.tasks.testing.Test
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
    analysisFile: File,
    jsLogger: JSLogger,
    listeners: TestListeners
  ): Unit =

    val analysis: Analysis = FileAnalysisStore
      .getDefault(analysisFile)
      .get
      .get
      .getAnalysis
      .asInstanceOf[Analysis]

    val testAdapterConfig: TestAdapter.Config = TestAdapter.Config()
      .withLogger(jsLogger)

    val loadedFrameworks: Seq[Framework] = TestAdapter(
      jsEnv = jsEnv,
      input = Seq(input),
      config = testAdapterConfig
    )
      .loadFrameworks(frameworkNames = TestFramework.all.map(_.implClassNames.toList))
      .flatten

    val framework2tests: Seq[(Framework, Seq[TestDefinition])] = getFramework2tests(
      detectors = getDetectors(loadedFrameworks).flatten,
      definitions = getDefinitions(analysis)
    ).toList

    // TODO report start/end for each framework and overall?
    val task: Task[Map[String, TestResult]] = if framework2tests.isEmpty then Task(Map.empty) else
      for
        startTime: Long <- Task(System.currentTimeMillis)
        overallTest: TestSuiteDescriptor = TestSuiteDescriptor("All tests")
        _ <- Task(listeners.startGroup(overallTest, startTime))

        // TODO merge the fors
        frameworkTasks: Seq[Task[Map[String, TestResult]]] =
          for (framework: Framework, tests: Seq[TestDefinition]) <- framework2tests yield
            val startTime: Long = System.currentTimeMillis
            val frameworkTest = TestSuiteDescriptor(s"${framework.name} tests")
            val testRunner: TestRunner = TestRunner(framework, listeners)
            for
              _ <- Task(listeners.startGroup(frameworkTest, startTime))
              testTasks: Seq[Task[Map[String, TestResult]]] = testRunner.toTasks(tests)
              results: Map[String, TestResult] <- Tests.combineSuiteResults(testTasks)
              _ <- Task(listeners.endGroup(frameworkTest, results))
              // Note: summary is ignored - endGroup() reconstructs it
              summary: String <- Task(testRunner.done())
            yield
              results

        results: Map[String, TestResult] <- Tests.combineSuiteResults(frameworkTasks)

        _ <- Task(listeners.endGroup(overallTest, results))
      yield
        results

    val report: Map[String, TestResult] = task.run()

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

  private def getFramework2tests(
    detectors: Seq[Detector],
    definitions: Seq[Definition]
  ): Map[Framework, Seq[TestDefinition]] =
    val result: Seq[(Framework, TestDefinition)] =
      for
        (definition: Definition, discovered: Discovered) <-
          Discovery(
            detectors.filter(detector => !detector.isAnnotation).map(_.name).toSet,
            detectors.filter(detector =>  detector.isAnnotation).map(_.name).toSet
          )(definitions)
        detector: Detector <- detectors.filter(_.isDetected(discovered))
      // TODO: detect Suite hierarchy? Unlikely...
      // TODO: To pass in correct explicitlySpecified and selectors
      yield detector.framework -> TestDefinition(
        isComposite = true, // this is a class
        taskDef = TaskDef(
          definition.name,
          detector.fingerprint,
          false,
          Array(new SuiteSelector)
        )
      )

    Collections.mapValues(result.groupBy(_._1))(_.map(_._2))

  given CanEqual[TStatus, TStatus] = CanEqual.derived

  def toTestResultType(status: TStatus): ResultType = status match
    case TStatus.Success  => ResultType.SUCCESS
    case TStatus.Error    => ResultType.FAILURE
    case TStatus.Failure  => ResultType.FAILURE
    case TStatus.Skipped  => ResultType.SKIPPED
    case TStatus.Ignored  => ResultType.SKIPPED
    case TStatus.Canceled => ResultType.SKIPPED
    case TStatus.Pending  => ResultType.SKIPPED

  given CanEqual[ResultType, ResultType] = CanEqual.derived

  // TODO is there something like that in Gradle?
  def max(left: ResultType, right: ResultType): ResultType = (left, right) match
    case (ResultType.FAILURE, _                 ) => ResultType.FAILURE
    case (_                 , ResultType.FAILURE) => ResultType.FAILURE
    case (ResultType.SKIPPED, _                 ) => ResultType.SKIPPED
    case (_                 , ResultType.SKIPPED) => ResultType.SKIPPED
    case _                                        => ResultType.SUCCESS

  val emptyTestResult: TestResult = new DefaultTestResult(
    ResultType.SUCCESS,
    Long.MaxValue,
    0,
    0,
    0,
    0,
    List.empty.asJava
  )

  def addTestResults(to: TestResult, from: TestResult): TestResult = DefaultTestResult(
    max(to.getResultType, from.getResultType),
    Math.min(to.getStartTime, from.getStartTime),
    Math.max(to.getEndTime, from.getEndTime),
    to.getTestCount + from.getTestCount,
    to.getSuccessfulTestCount + from.getSuccessfulTestCount,
    to.getFailedTestCount + from.getFailedTestCount,
    (to.getExceptions.asScala ++ from.getExceptions.asScala).asJava
  )

  def combineSuiteResults(tasks: Seq[Task[Map[String, TestResult]]]): Task[Map[String, TestResult]] =
    for suiteResults: Seq[Map[String, TestResult]] <- Task.join(tasks)
    yield suiteResults.foldLeft(Map.empty[String, TestResult]) {
      case (sum: Map[String, TestResult], e: Map[String, TestResult]) =>
        val grouped: Map[String, Seq[(String, TestResult)]] = (sum.toSeq ++ e.toSeq).groupBy(_._1)
        Collections.mapValues(grouped)(combineTestResults)
    }

  // TODO take overall start/end time parameters
  def combineTestResults(results: Seq[(String, TestResult)]): TestResult =
    results.map(_._2).foldLeft(emptyTestResult)(addTestResults)
