package org.podval.tools.build

sealed trait ScalaBackend:
  def suffix: Option[String]

  final def suffixString: String = suffix match
    case None => ""
    case Some(suffix) => s"_$suffix"

object ScalaBackend:
  case object Jvm extends ScalaBackend:
    override def suffix: Option[String] = None
  
  case object JS extends ScalaBackend:
    override def suffix: Option[String] = Some("sjs1")
