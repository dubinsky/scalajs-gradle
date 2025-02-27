package org.podval.tools.test

import org.gradle.api.tasks.testing.TestResult.ResultType
import org.gradle.internal.id.CompositeIdGenerator.CompositeId
import org.podval.tools.util.Scala212Collections.{arrayMap, arrayMkString, arrayZipForAll}
import sbt.testing.{AnnotatedFingerprint, Fingerprint, NestedSuiteSelector, NestedTestSelector, Selector, Status, 
  SubclassFingerprint, SuiteSelector, Task, TaskDef, TestSelector, TestWildcardSelector}

object TestInterface:
  // Note: Since I can not use the real `rootTestSuiteId` that `DefaultTestExecuter` supplies to the `TestMainAction` -
  // because it is a `String` - and I am not keen on second-guessing what it is anyway,
  // I use a `idPlaceholder` in `WorkerTestClassProcessor`
  // and change it to the real one in `FixUpRootTestOutputTestResultProcessor`.
  val rootTestSuiteIdPlaceholder: CompositeId = CompositeId(0L, 0L)
  
  def toString(task: Task): String = toString(task.taskDef)
  def toString(taskDef: TaskDef): String =
    def className(isModule: Boolean): String = taskDef.fullyQualifiedName + (if isModule then "$" else "")

    val name: String = taskDef.fingerprint match
      case annotated: AnnotatedFingerprint => s"@${annotated.annotationName} ${className(annotated.isModule)}"
      case subclass: SubclassFingerprint => s"${className(subclass.isModule)} extends ${subclass.superclassName}"

    val selectors: String = arrayMkString(arrayMap(taskDef.selectors, _.toString), "[", ", ", "]")

    s"$name selectors=$selectors explicitlySpecified=${taskDef.explicitlySpecified}"

  def getSelector(task: Task): Selector =
    val taskDef: TaskDef = task.taskDef
    require(taskDef.selectors.length == 1, "Exactly one Selector is required")
    taskDef.selectors()(0)

  def isTest(selector: Selector): Boolean = selector match
    case _: TestSelector | _: NestedTestSelector | _: TestWildcardSelector => true
    case _ => false

  // Selector subclasses are final and override equals(),
  // so `left.equals(right)` should work just fine,
  // but with ScalaCheck running on ScalaJS (but not plain Scala)
  // I get TestSelector(String.startsWith) != TestSelector(String.startsWith) -
  // for every test method, even other than `String.startsWith`, so...
  def selectorsEqual(left: Selector, right: Selector): Boolean =
    val result: Boolean = (left, right) match
      case (_: SuiteSelector, _: SuiteSelector) => true
      case (left: NestedSuiteSelector, right: NestedSuiteSelector) => left.suiteId == right.suiteId
      case (left: TestSelector, right: TestSelector) => left.testName == right.testName
      case (left: NestedTestSelector, right: NestedTestSelector) => (left.suiteId == right.suiteId) && left.testName == right.testName()
      case (left: TestWildcardSelector, right: TestWildcardSelector) => left.testWildcard == right.testWildcard
      case _ => false

    //    require(result == left.equals(right),
    //      s"--- SELECTOR COMPARISON DISCREPANCY: $left [${left.getClass}] and $right [${right.getClass}]"
    //    )

    result

  def taskDefsEqual(left: TaskDef, right: TaskDef): Boolean =
    left.fullyQualifiedName == right.fullyQualifiedName &&
    fingerprintsEqual(left.fingerprint, right.fingerprint) &&
    left.explicitlySpecified == right.explicitlySpecified &&
    left.selectors.length == right.selectors.length &&
    arrayZipForAll(left.selectors, right.selectors, selectorsEqual)

  // Note: I can't rely on all the frameworks providing equals() on their Fingerprint implementations...
  private def fingerprintsEqual(left: Fingerprint, right: Fingerprint): Boolean = (left, right) match
    case (left: AnnotatedFingerprint, right: AnnotatedFingerprint) =>
      left.annotationName == right.annotationName &&
      left.isModule == right.isModule
    case (left: SubclassFingerprint, right: SubclassFingerprint) =>
      left.superclassName == right.superclassName &&
      left.isModule == right.isModule &&
      left.requireNoArgConstructor == right.requireNoArgConstructor
    case _ => false

  def toResultType(status: Status): ResultType =
    // When `scalajs-test-interface` is used instead of the `test-interface`, I get:
    //   Class sbt.testing.Status does not have member field 'sbt.testing.Status Success'
    //    given CanEqual[Status, Status] = CanEqual.derived
    //    status match
    //    case Status.Success  => ResultType.SUCCESS
    //    case Status.Error    => ResultType.FAILURE
    //    case Status.Failure  => ResultType.FAILURE
    //    case Status.Skipped  => ResultType.SKIPPED
    //    case Status.Ignored  => ResultType.SKIPPED
    //    case Status.Canceled => ResultType.SKIPPED
    //    case Status.Pending  => ResultType.SKIPPED
    // This approach works for both:
    val name: String = status.name()
    if name == "Success" then ResultType.SUCCESS else
    if name == "Error" then ResultType.FAILURE else 
    if name == "Failure" then ResultType.FAILURE else
      ResultType.SKIPPED
