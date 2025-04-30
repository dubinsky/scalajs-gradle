package org.podval.tools.build

import org.podval.tools.node.NodeDependency

sealed abstract class ScalaBackendKind(
  val name: String,
  val displayName: String,
  val suffix: Option[String],
  val testsCanNotBeForked: Boolean
) derives CanEqual:
  final def suffixString: String = suffix match
    case None => ""
    case Some(suffix) => s"_$suffix"

object ScalaBackendKind:
  def all: Set[ScalaBackendKind] = Set(JVM, JS, Native)
  
  case object JVM extends ScalaBackendKind(
    name = "JVM",
    displayName = "JVM",
    suffix = None,
    testsCanNotBeForked = false
  )

  case object JS extends ScalaBackendKind(
    name = "JS",
    displayName = "Scala.js",
    suffix = Some("sjs1"),
    testsCanNotBeForked = true
  )

  case object Native extends ScalaBackendKind(
    name = "Native",
    displayName = "Scala Native",
    suffix = Some("native0.5"),
    testsCanNotBeForked = true
  )
