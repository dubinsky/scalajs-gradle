package org.podval.tools.test.testproject

import org.gradle.api.tasks.testing.TestResult.ResultType

final class ForClass(
  val className: String,
  val expectations: Seq[ForClass.ClassExpectation]
)

object ForClass:
  val testPackage: String = "org.podval.tools.test"

  def forClass(className: String, expectations: ClassExpectation*): ForClass = ForClass(className, expectations)

  enum ClassExpectation derives CanEqual:
    case AbsentClass
    case PresentClass
    case TestCount   (count: Int)
    case FailedCount (count: Int)
    case SkippedCount(count: Int)
    case Method(methodName: String, methodExpectation: MethodExpectation)

  import ClassExpectation.*

  def absentClass : AbsentClass .type = AbsentClass
  def presentClass: PresentClass.type = PresentClass
  def testCount   (count: Int): TestCount    = TestCount   (count)
  def failedCount (count: Int): FailedCount  = FailedCount (count)
  def skippedCount(count: Int): SkippedCount = SkippedCount(count)

  enum MethodExpectation derives CanEqual:
    case AbsentMethod
    case PresentMethod
    case MethodResult(resultType: ResultType)

  import MethodExpectation.*

  def absent (methodName: String): Method = Method(methodName, AbsentMethod )
  def present(methodName: String): Method = Method(methodName, PresentMethod)
  def passed (methodName: String): Method = Method(methodName, MethodResult(ResultType.SUCCESS))
  def failed (methodName: String): Method = Method(methodName, MethodResult(ResultType.FAILURE))
  def skipped(methodName: String): Method = Method(methodName, MethodResult(ResultType.SKIPPED))
