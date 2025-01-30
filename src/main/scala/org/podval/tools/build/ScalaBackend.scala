package org.podval.tools.build

sealed trait ScalaBackend:
  final def suffixString: String = suffix match
    case None => ""
    case Some(suffix) => s"_$suffix"
    
  def suffix: Option[String]

object ScalaBackend:
  case object Jvm extends ScalaBackend:
    override def suffix: Option[String] = None
  
  case object JS extends ScalaBackend:
    override def suffix: Option[String] = Some("sjs1")
