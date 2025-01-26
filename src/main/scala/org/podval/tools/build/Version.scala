package org.podval.tools.build

import scala.CanEqual

final class Version(val version: String) derives CanEqual:
  private val segments: Array[String] = version.split('.')

  override def toString: String = version

  override def equals(other: Any): Boolean = other match
    case that: Version => this.version == that.version
    case _ => false

  def major: Int = segments(0).toInt
    
  def majorMinorMicro: (Int, Int, Int) =
    (
      segments(0).toInt,
      segments(1).toInt,
      segments(2).toInt
    )
    
  def majorAndMinorString: String =
    segments.take(2).mkString(".")
