package org.podval.tools.scalajs.testing

import org.opentorah.util.Collections
import org.gradle.api.logging.Logger
import sbt.internal.inc.Analysis
import sbt.testing.{AnnotatedFingerprint, Fingerprint, Framework, Runner, Selector, SubclassFingerprint, SuiteSelector, TaskDef, Task as TestTask}
import xsbt.api.{Discovered, Discovery}
import xsbti.api.{AnalyzedClass, ClassLike, Companions, Definition}
import xsbti.compile.CompileAnalysis

// Note: based on sbt.Tests from org.scala-sbt.actions
object Tests:

  def discover(
    frameworks: Seq[Framework],
    analysis: CompileAnalysis,
    log: Logger
  ): Seq[TestDefinition] /*(Seq[TestDefinition], Set[String])*/ = discover(
    fingerprints = frameworks.flatMap(TestFramework.getFingerprints),
    definitions = allDefs(analysis.asInstanceOf[Analysis]),
    log = log
  )

  private def allDefs(analysis: Analysis): Seq[Definition] =
    val acs: Seq[AnalyzedClass] = analysis.apis.internal.values.toVector
    acs.flatMap { (ac: AnalyzedClass) =>
      val companions: Companions = ac.api
      Seq(companions.classApi: Definition, companions.objectApi: Definition) ++
      companions.classApi .structure.declared .toSeq ++
      companions.classApi .structure.inherited.toSeq ++
      companions.objectApi.structure.declared .toSeq ++
      companions.objectApi.structure.inherited.toSeq
    }

  private def discover(
    fingerprints: Seq[Fingerprint],
    definitions: Seq[Definition],
    log: Logger
  ): Seq[TestDefinition] = //(Seq[TestDefinition], Set[String]) =
    def debug(message: String): Unit = log.debug(message, null, null, null)

    val subclasses: Seq[(String, Boolean, SubclassFingerprint)] = fingerprints.collect {
      case sub: SubclassFingerprint => (sub.superclassName, sub.isModule, sub)
    }
    val annotations: Seq[(String, Boolean, AnnotatedFingerprint)] = fingerprints.collect {
      case ann: AnnotatedFingerprint => (ann.annotationName, ann.isModule, ann)
    }
    debug("Subclass fingerprints: " + subclasses)
    debug("Annotation fingerprints: " + annotations)

    def firsts[A, B, C](s: Seq[(A, B, C)]): Set[A] = s.map(_._1).toSet
    def defined(
      in: Seq[(String, Boolean, Fingerprint)],
      names: Set[String],
      IsModule: Boolean
    ): Seq[Fingerprint] =
      in.collect { case (name, IsModule, print) if names(name) => print }

    def toFingerprints(d: Discovered): Seq[Fingerprint] =
      defined(subclasses , d.baseClasses, d.isModule) ++
      defined(annotations, d.annotations, d.isModule)

    val discovered: Seq[(Definition, Discovered)] = Discovery(firsts(subclasses), firsts(annotations))(definitions.filter {
      case c: ClassLike => c.topLevel
      case _            => false
    })
    // TODO: To pass in correct explicitlySpecified and selectors
    val tests: Seq[TestDefinition] =
      for
        (df: Definition, di: Discovered) <- discovered
        fingerprint <- toFingerprints(di)
      yield new TestDefinition(
        name = df.name,
        fingerprint = fingerprint,
        explicitlySpecified = false,
        selectors = Array(new SuiteSelector: Selector)
      )
//    val mains = discovered.collect { case (df, di) if di.hasMain => df.name }
//    (tests, mains.toSet)
    tests

  def apply(
    frameworks: Map[TestFramework, Framework],
    runners: Map[TestFramework, Runner],
    tests: Seq[TestDefinition],
    listeners: Seq[TestsListener],
    log: Logger
  ): Task[Output] =
    def fj(action: () => Unit): Task[Unit] = new Task(action)

    val (
      frameworkSetup,//: () => Unit,
      runnables,//: Seq[TestRunnable],
      frameworkCleanup//: TestResult => () => Unit
    ) = TestFramework.testTasks(
      frameworks,
      runners,
      tests,
      log,
      listeners
    )

    fj(frameworkSetup)
    .flatMap(_ => toTasks(runnables)) // dependsOn()
    .map(_.toList)
    .map(processResults)
    .flatMap((output: Output) => fj(frameworkCleanup(output.overall)).map(_ => output))

  private def processResults(results: Seq[(String, SuiteResult)]): Output =
    Output(overall(results.map(_._2.result)), results.toMap, Seq.empty)

  private def overall(results: Seq[TestResult]): TestResult =
    results.foldLeft[TestResult](TestResult.Passed)((acc, result) =>
      if acc.severity < result.severity then result else acc
    )

  type TestRunnable = (String, TestFunction)

  def toTasks(runnables: Seq[TestRunnable]): Task[Map[String, SuiteResult]] =
    val tasks: Seq[Task[Map[String, SuiteResult]]] =
      for (name: String, test: TestFunction) <- runnables yield toTask(name, test)
    Task.join(tasks).map(_.foldLeft(Map.empty[String, SuiteResult]) {
      case (sum: Map[String, SuiteResult], e: Map[String, SuiteResult]) =>
        val merged: Seq[(String, SuiteResult)] = sum.toSeq ++ e.toSeq
        val grouped: Map[String, Seq[(String, SuiteResult)]] = merged.groupBy(_._1)
        Collections.mapValues(grouped)(_.map(_._2).foldLeft(SuiteResult.Empty) {
          case (resultSum, result) => resultSum + result
        })
    })

  def toTask(
    name: String,
    fun: TestFunction
  ): Task[Map[String, SuiteResult]] =
    new Task[(String, (SuiteResult, Seq[TestTask]))](
      () => (name, fun.apply())
    )
      .flatMap { case (name: String, (result: SuiteResult, nested: Seq[TestTask])) =>
        val nestedRunnables: Seq[TestRunnable] = createNestedRunnables(fun, nested)
        toTasks(nestedRunnables).map { (currentResultMap: Map[String, SuiteResult]) =>
          val newResult: SuiteResult = currentResultMap.get(name) match
            case Some(currentResult) => currentResult + result
            case None                => result
          currentResultMap.updated(name, newResult)
        }
    }

  private def createNestedRunnables(
    testFun: TestFunction,
    nestedTasks: Seq[TestTask]
  ): Seq[TestRunnable] =
    for (nt: TestTask, idx: Int) <- nestedTasks.zipWithIndex yield
      val testFunDef: TaskDef = testFun.taskDef
      (
        testFunDef.fullyQualifiedName,
        TestFramework.createTestFunction(
          new TaskDef(
            testFunDef.fullyQualifiedName + "-" + idx,
            testFunDef.fingerprint,
            testFunDef.explicitlySpecified,
            testFunDef.selectors
          ),
          testFun.runner,
          nt
        )
      )
