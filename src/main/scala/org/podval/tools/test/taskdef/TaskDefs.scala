package org.podval.tools.test.taskdef

import sbt.testing.{AnnotatedFingerprint, Fingerprint, Selector, SubclassFingerprint, TaskDef}

// TODO remove Ops and implement equal/toString
object TaskDefs extends Ops[TaskDef]("#"):
  def toString(taskDef: TaskDef): String =
    def className(isModule: Boolean): String = taskDef.fullyQualifiedName + (if isModule then "$" else "")

    val name: String = taskDef.fingerprint match
      case annotated: AnnotatedFingerprint => s"@${annotated.annotationName} ${className(annotated.isModule)}"
      case subclass : SubclassFingerprint  => s"${className(subclass.isModule)} extends ${subclass.superclassName}"

    s"$name selectors=${Selectors.Many.toString(taskDef.selectors)} explicitlySpecified=${taskDef.explicitlySpecified}"

  override protected def toStrings(value: TaskDef): Array[String] =
    val fingerprint: String = Fingerprints.write(value.fingerprint)
    val selectors: String = Selectors.Many.write(value.selectors)
    
    Array(
      value.fullyQualifiedName,
      Ops.toString(value.explicitlySpecified),
      fingerprint,
      selectors
    )

  override protected def fromStrings(strings: Array[String]): TaskDef =
    val fullyQualifiedName: String = strings(0)
    val explicitlySpecified: Boolean = Ops.toBoolean(strings(1))
    val fingerprint: Fingerprint = Fingerprints.read(strings(2))
    val selectors: Array[Selector] = Selectors.Many.read(strings(3))
      
    TaskDef(
      fullyQualifiedName,
      fingerprint,
      explicitlySpecified,
      selectors
    )
