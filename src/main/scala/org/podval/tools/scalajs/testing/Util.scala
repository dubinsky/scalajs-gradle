package org.podval.tools.scalajs.testing

import org.gradle.api.tasks.testing.TestResult.ResultType
import sbt.testing.{AnnotatedFingerprint, Fingerprint, SubclassFingerprint, TaskDef, Task as TestTask}

// TODO is there something like that in Gradle?
// TODO dissolve
object Util:
  given CanEqual[ResultType, ResultType] = CanEqual.derived

  def max(left: ResultType, right: ResultType): ResultType = (left, right) match
    case (ResultType.FAILURE, _                 ) => ResultType.FAILURE
    case (_                 , ResultType.FAILURE) => ResultType.FAILURE
    case (ResultType.SKIPPED, _                 ) => ResultType.SKIPPED
    case (_                 , ResultType.SKIPPED) => ResultType.SKIPPED
    case _                                        => ResultType.SUCCESS

  def toString(f: Fingerprint): String = f match
    case sf: SubclassFingerprint  =>
      s"SubclassFingerprint(isModule=${sf.isModule}, superclassName=${sf.superclassName}, requireNoArgConstructor=${sf.requireNoArgConstructor})"
    case af: AnnotatedFingerprint =>
      s"AnnotatedFingerprint(isModule=${af.isModule}, annotationName=${af.annotationName})"
//    case _                        => f.toString

  def toString(testTask: TestTask): String =
    s"""
       |TestTask(
       |  taskDef=${toString(testTask.taskDef)},
       |  tags=${testTask.tags.toList}
       |)
       |""".stripMargin

  def toString(taskDef: TaskDef): String =
    s"""
       |TaskDef(
       | fullyQualifiedName=${taskDef.fullyQualifiedName},
       | fingerprint=${toString(taskDef.fingerprint)},
       | explicitlySpecified=${taskDef.explicitlySpecified},
       | selectors=${taskDef.selectors.toList}
       |)
       |""".stripMargin

  def toString(testEvent: TestEvent): String =
    s"""TestEvent(
       |  fullyQualifiedName=${testEvent.fullyQualifiedName},
       |  fingerprint=${Util.toString(testEvent.fingerprint)},
       |  selector=${testEvent.selector},
       |  status=${testEvent.status},
       |  throwable=${testEvent.throwable},
       |  duration=${testEvent.duration}
       |)
       |""".stripMargin
