package org.podval.tools.build

import org.podval.tools.node.NodeDependency

sealed trait ScalaBackend:
  def isJS: Boolean
  def toJvm: ScalaBackend
  def suffix: Option[String]

  final def suffixString: String = suffix match
    case None => ""
    case Some(suffix) => s"_$suffix"

object ScalaBackend:
  case object Jvm extends ScalaBackend:
    override def isJS: Boolean = false
    override def toJvm: ScalaBackend = this
    override def suffix: Option[String] = None
  
  final case class JS(nodeVersion: Version = NodeDependency.versionDefault) extends ScalaBackend:
    override def isJS: Boolean = true
    override def toJvm: ScalaBackend = Jvm
    override def suffix: Option[String] = Some("sjs1")
