package org.podval.tools.testing.worker

import org.gradle.api.internal.tasks.testing.{DefaultTestClassDescriptor, DefaultTestMethodDescriptor, TestDescriptorInternal}
import org.gradle.internal.id.CompositeIdGenerator.CompositeId
import sbt.testing.{AnnotatedFingerprint, Fingerprint, NestedTestSelector, Selector, SubclassFingerprint, TaskDef,
  TestSelector, TestWildcardSelector}

object TaskDefEx:
  // Note: Since I can not use the real `rootTestSuiteId` that `DefaultTestExecuter` supplies to the `TestMainAction` -
  // because it is a `String` - and I am not keen on second-guessing what it is anyway,
  // I use a placeholder id and change it to the real one in `FixUpRootTestOutputTestResultProcessor`.
  val rootTestSuiteIdPlaceholder: CompositeId = CompositeId(0L, 0L)

  // TODO [nested] handle nested suits
  def toTestDescriptorInternal(id: AnyRef, taskDef: TaskDef): TestDescriptorInternal =
    methodName(taskDef) match
      case None             => DefaultTestClassDescriptor (id, taskDef.fullyQualifiedName)
      case Some(methodName) => DefaultTestMethodDescriptor(id, taskDef.fullyQualifiedName, methodName)

  // Note: in reality, an individual test is not always a method (e.g., in ScalaTest), but compared to a class it is :)
  def verifyCanHaveNestedTest(taskDef: TaskDef, nestedTaskDef: TaskDef): Unit =
    val selectors: Array[Selector] = nestedTaskDef.selectors
    require(selectors.nonEmpty, s"No selectors in nested TaskDef: $nestedTaskDef")
    require(selectors.length == 1, s"More than one selector in nested TaskDef: TaskDef")
    require(!selectors.head.isInstanceOf[TestWildcardSelector], "Encountered TestWildcardSelector!")
    require(methodName(taskDef).isEmpty, "Method tests can not have nested tests")
    require(methodName(nestedTaskDef).nonEmpty, "Only method tests can be nested")

  private def methodName(taskDef: TaskDef): Option[String] =
    // TODO I block multiple selectors on a task!
    if taskDef.selectors.length != 1 then None else taskDef.selectors.head match
      case testSelector: TestSelector => Some(testSelector.testName)
      case nestedTestSelector: NestedTestSelector => Some(nestedTestSelector.testName) // Note: UTest uses this
      case _ => None

  def taskDefsEqual(left: TaskDef, right: TaskDef): Boolean =
    left.fullyQualifiedName == right.fullyQualifiedName &&
    fingerprintsEqual(left.fingerprint, right.fingerprint) &&
    left.explicitlySpecified == right.explicitlySpecified &&
    left.selectors.length == right.selectors.length &&
    left.selectors.zip(right.selectors).forall((left: Selector, right: Selector) => left.equals(right))

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

  def toString(taskDef: TaskDef): String =
    def className(isModule: Boolean): String = taskDef.fullyQualifiedName + (if isModule then "$" else "")
    val name: String = taskDef.fingerprint match
      case annotated: AnnotatedFingerprint => s"@${annotated.annotationName} ${className(annotated.isModule)}"
      case subclass: SubclassFingerprint   => s"${className(subclass.isModule)} extends ${subclass.superclassName}"

    val selectors: String = taskDef.selectors.map(_.toString).mkString("[", ", ", "]")
      s"$name explicitlySpecified=${taskDef.explicitlySpecified} selectors=$selectors"

