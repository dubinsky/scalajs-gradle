package org.podval.tools.scalajs.testing

import org.gradle.api.logging.Logger
import org.opentorah.util.Collections
import sbt.testing.{AnnotatedFingerprint, Fingerprint, Framework, Runner, SubclassFingerprint, Task as TestTask, TaskDef}
import scala.util.control.NonFatal

// Note: based on sbt.TestFramework from org.scala-sbt.testing
final class TestFramework(val implClassNames: String*) derives CanEqual:
  override def equals(o: Any): Boolean = o.asInstanceOf[Matchable] match
    case x: TestFramework => (this.implClassNames.toList == x.implClassNames.toList)
    case _                => false

  override def hashCode: Int =
    37 * (17 + implClassNames.##) + "TestFramework".##

  override def toString: String =
    "TestFramework(" + implClassNames.mkString(", ") + ")"

// TODO add whatever other test frameworks are supported by ScalaJS
object TestFramework:
  val ScalaCheck: TestFramework = TestFramework(
    "org.scalacheck.ScalaCheckFramework"
  )
  val ScalaTest: TestFramework = TestFramework(
    "org.scalatest.tools.Framework",
    "org.scalatest.tools.ScalaTestFramework"
  )
  val Specs: TestFramework = TestFramework(
    "org.specs.runner.SpecsFramework"
  )
  val Specs2: TestFramework = TestFramework(
    "org.specs2.runner.Specs2Framework",
    "org.specs2.runner.SpecsFramework"
  )
  val JUnit: TestFramework = TestFramework(
    "com.novocode.junit.JUnitFramework"
  )
  val MUnit: TestFramework = TestFramework(
    "munit.Framework"
  )

  val all: List[TestFramework] = List(ScalaCheck, Specs2, Specs, ScalaTest, JUnit, MUnit)

  def getFingerprints(framework: Framework): Seq[Fingerprint] =
    // TODO why is reflection used instead of the direct call?
    framework.getClass.getMethod("fingerprints").invoke(framework) match
      case fingerprints: Array[Fingerprint] => fingerprints.toList
      case _                                => sys.error(s"Could not call 'fingerprints' on framework $framework")

  def safeForeach[T](it: Iterable[T], log: Logger)(f: T => Unit): Unit = it.foreach(i =>
    try f(i)
    catch { case NonFatal(e) => log.trace("", e); log.error(e.toString) } // TODO the message
  )

  def hashCode(f: Fingerprint): Int = f match
    case s: SubclassFingerprint  => (s.isModule, s.superclassName).hashCode
    case a: AnnotatedFingerprint => (a.isModule, a.annotationName).hashCode
    case _                       => 0

  def matches(a: Fingerprint, b: Fingerprint): Boolean = (a, b) match
    case (a: SubclassFingerprint, b: SubclassFingerprint) =>
      a.isModule == b.isModule && a.superclassName == b.superclassName
    case (a: AnnotatedFingerprint, b: AnnotatedFingerprint) =>
      a.isModule == b.isModule && a.annotationName == b.annotationName
    case _ => false

  def toString(f: Fingerprint): String = f match
    case sf: SubclassFingerprint  => s"subclass(${sf.isModule}, ${sf.superclassName})"
    case af: AnnotatedFingerprint => s"annotation(${af.isModule}, ${af.annotationName})"
    case _                        => f.toString

  def filterFrameworks(
    loadedFrameworks: Map[TestFramework, Framework],
    tests: Seq[TestDefinition]
  ): Map[TestFramework, Framework] = loadedFrameworks.filter { case (_, framework) =>
    getFingerprints(framework).exists((fingerprint: Fingerprint) =>
      tests.exists((test: TestDefinition) =>
        matches(fingerprint, test.fingerprint)
      )
    )
  }

  def testTasks(
    frameworks: Map[TestFramework, Framework],
    runners: Map[TestFramework, Runner],
    tests: Seq[TestDefinition],
    log: Logger,
    listeners: Seq[TestsListener]
  ): (() => Unit, Seq[(String, TestFunction)], TestResult => () => Unit) =
    val mappedTests = mapTests(frameworks.values.toSeq, tests)
    if mappedTests.isEmpty
    then
      (() => (), Vector(), _ => () => ())
    else
      createTestTasks(
        for (testFramework: TestFramework, runner: Runner) <- runners yield
          (frameworks(testFramework), new TestRunner(runner, listeners, log)),
        mappedTests,
        tests,
        log,
        listeners
      )

  private def createTestTasks(
    runners: Map[Framework, TestRunner],
    tests: Map[Framework, Set[TestDefinition]],
    ordered: Seq[TestDefinition],
    log: Logger,
    listeners: Seq[TestsListener]
  ): (() => Unit, Seq[(String, TestFunction)], TestResult => (() => Unit)) =

    def foreachListenerSafe(f: TestsListener => Unit): () => Unit =
      () => safeForeach(listeners, log)(f)

    val startTask: () => Unit = foreachListenerSafe(_.doInit())

    val testTasks: Map[String, TestFunction] = Map(tests.toSeq.flatMap { case (framework, testDefinitions) =>
      val runner: TestRunner = runners(framework)
      for testTask: TestTask <- runner.tasks(testDefinitions) yield
        val taskDef: TaskDef = testTask.taskDef
        (taskDef.fullyQualifiedName, createTestFunction(taskDef, runner, testTask))
    }*)

    val endTask: TestResult => () => Unit = (result: TestResult) => foreachListenerSafe(_.doComplete(result))

    val order: Seq[(String, TestFunction)] =
      for
        d <- ordered
        act <- testTasks.get(d.name)
      yield (d.name, act)

    (startTask, order, endTask)

  private def mapTests(
    frameworks: Seq[Framework],
    tests: Seq[TestDefinition]
  ): Map[Framework, Set[TestDefinition]] =
    import collection.mutable

    val map: mutable.Map[Framework, mutable.Set[TestDefinition]] =
      new mutable.HashMap[Framework, mutable.Set[TestDefinition]]

    def assignTest(test: TestDefinition): Unit =
      def isTestForFramework(framework: Framework): Boolean =
        getFingerprints(framework).exists((fingerprint: Fingerprint) => matches(fingerprint, test.fingerprint))

      for (framework <- frameworks.find(isTestForFramework))
        map.getOrElseUpdate(framework, new mutable.HashSet[TestDefinition]) += test

    if frameworks.nonEmpty then tests.foreach(assignTest)

    Collections.mapValues(map.toMap)(_.toSet)

  // TODO move
  def createTestFunction(
    taskDef: TaskDef,
    runner: TestRunner,
    testTask: TestTask
  ): TestFunction = new TestFunction(
    taskDef,
    runner,
    (r: TestRunner) => r.run(taskDef, testTask)
  ):
    override def tags: Seq[String] = testTask.tags.toIndexedSeq
