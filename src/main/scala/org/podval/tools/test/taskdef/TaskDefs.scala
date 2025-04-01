package org.podval.tools.test.taskdef

import org.podval.tools.util.Scala212Collections.{arrayMap, arrayMkString, arrayZipForAll}
import sbt.testing.{AnnotatedFingerprint, SubclassFingerprint, TaskDef}

object TaskDefs:
  def equal(left: TaskDef, right: TaskDef): Boolean =
    (left.fullyQualifiedName == right.fullyQualifiedName) &&
    (left.explicitlySpecified == right.explicitlySpecified) &&
    Fingerprints.equal(left.fingerprint, right.fingerprint) &&
    arrayZipForAll(left.selectors, right.selectors, Selectors.equal)

  def toString(taskDef: TaskDef): String =
    def className(isModule: Boolean): String = taskDef.fullyQualifiedName + (if isModule then "$" else "")

    val name: String = taskDef.fingerprint match
      case annotated: AnnotatedFingerprint => s"@${annotated.annotationName} ${className(annotated.isModule)}"
      case subclass : SubclassFingerprint  => s"${className(subclass.isModule)} extends ${subclass.superclassName}"

    val selectors: String = arrayMkString(arrayMap(taskDef.selectors, _.toString), "[", ", ", "]")
    s"$name selectors=$selectors explicitlySpecified=${taskDef.explicitlySpecified}"
