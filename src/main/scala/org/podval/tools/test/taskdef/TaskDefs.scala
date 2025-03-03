package org.podval.tools.test.taskdef

import org.podval.tools.util.Scala212Collections.{arrayMap, arrayZipForAll}
import sbt.testing.{AnnotatedFingerprint, Fingerprint, Selector, SubclassFingerprint, TaskDef}

object TaskDefs:
  def toString(taskDef: TaskDef): String =
    def className(isModule: Boolean): String = taskDef.fullyQualifiedName + (if isModule then "$" else "")

    // TODO move
    val name: String = taskDef.fingerprint match
      case annotated: AnnotatedFingerprint => s"@${annotated.annotationName} ${className(annotated.isModule)}"
      case subclass: SubclassFingerprint => s"${className(subclass.isModule)} extends ${subclass.superclassName}"

    s"$name selectors=${Selectors.toString(taskDef.selectors)} explicitlySpecified=${taskDef.explicitlySpecified}"
  
  def taskDefsEqual(left: TaskDef, right: TaskDef): Boolean =
    left.fullyQualifiedName == right.fullyQualifiedName &&
    Fingerprints.equal(left.fingerprint, right.fingerprint) &&
    left.explicitlySpecified == right.explicitlySpecified &&
    left.selectors.length == right.selectors.length &&
    arrayZipForAll(left.selectors, right.selectors, Selectors.equal)
  
  def write(value: TaskDef): String =
    val fingerprint: String = Fingerprints.write(value.fingerprint)
    val selectors: String = value.selectors.map(Selectors.write).mkString("-")
    s"${value.fullyQualifiedName}#${value.explicitlySpecified}#$fingerprint#$selectors"

  def read(string: String): TaskDef =
    val parts: Array[String] = string.split("#")
    val fullyQualifiedName: String = parts(0)
    val explicitlySpecified: Boolean = parts(1) == "true"
    val fingerprint: Fingerprint = Fingerprints.read(parts(2))
    val selectors: Array[Selector] = arrayMap(parts(3).split("-"), Selectors.read)
      
    TaskDef(
      fullyQualifiedName,
      fingerprint,
      explicitlySpecified,
      selectors
    )
