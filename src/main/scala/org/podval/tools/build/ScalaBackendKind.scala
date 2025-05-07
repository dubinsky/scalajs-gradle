package org.podval.tools.build

import org.podval.tools.node.NodeDependency

sealed trait ScalaBackendKind derives CanEqual:
  def name: String
  def displayName: String
  def suffix: Option[String]
  def testsCanNotBeForked: Boolean

  final def suffixString: String = suffix match
    case None => ""
    case Some(suffix) => s"_$suffix"

object ScalaBackendKind:
  def all: Set[ScalaBackendKind] = Set(JVM, JS, Native)
  
  case object JVM extends ScalaBackendKind:
    override val name: String = "JVM"
    override val displayName: String = "JVM"
    override val suffix: Option[String] = None
    override val testsCanNotBeForked: Boolean = false

  sealed trait NonJvm extends ScalaBackendKind:
    def versionDefault: Version
    
  case object JS extends NonJvm:
    override val name: String = "JS"
    override val displayName: String = "Scala.js"
    override val suffix: Option[String] = Some("sjs1")
    override val testsCanNotBeForked: Boolean = true
    override val versionDefault: Version = Version("1.19.0")

  case object Native extends NonJvm:
    override val name: String = "Native"
    override val displayName: String = "Scala Native"
    override val suffix: Option[String] = Some("native0.5")
    override val testsCanNotBeForked: Boolean = true
    override val versionDefault: Version = Version("0.5.7")
