package org.podval.tools.build

import org.podval.tools.node.NodeDependency

sealed trait ScalaBackend:
  def kind: ScalaBackendKind
  override def toString: String = kind.toString

object ScalaBackend:
  case object JVM extends ScalaBackend:
    override def kind: ScalaBackendKind = ScalaBackendKind.JVM
  
  final case class JS(nodeVersion: Version = NodeDependency.versionDefault) extends ScalaBackend:
    override def kind: ScalaBackendKind = ScalaBackendKind.JS
    override def toString: String = s"$kind(Node $nodeVersion)"

  case object Native extends ScalaBackend:
    override def kind: ScalaBackendKind = ScalaBackendKind.Native
    