package org.podval.tools.build

import org.podval.tools.node.NodeDependency

sealed trait ScalaBackendKind derives CanEqual:
  def suffix: Option[String]

  final def suffixString: String = suffix match
    case None => ""
    case Some(suffix) => s"_$suffix"

object ScalaBackendKind:
  def all: Set[ScalaBackendKind] = Set(JVM, JS, Native)
  
  case object JVM extends ScalaBackendKind:
    override def suffix: Option[String] = None

  case object JS extends ScalaBackendKind:
    override def suffix: Option[String] = Some("sjs1")

  case object Native extends ScalaBackendKind:
    override def suffix: Option[String] = Some("sjs1") // TODO

    