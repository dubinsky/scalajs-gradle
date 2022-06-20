package org.podval.tools.scalajs

import org.gradle.api.logging.LogLevel as GLevel
import org.scalajs.logging.Level as JSLevel

final class JSLogger(gradleLogger: org.gradle.api.logging.Logger, source: String) extends org.scalajs.logging.Logger:

  private def logSource: String = s"ScalaJS $source"

  override def trace(t: => Throwable): Unit =
    gradleLogger.error(s"$logSource Error", t)

  override def log(level: JSLevel, message: => String): Unit =
    gradleLogger.log(JSLogger.scalajs2gradleLevel(level), s"$logSource: $message")

object JSLogger:
  given CanEqual[JSLevel, JSLevel] = CanEqual.derived

  private def scalajs2gradleLevel(level: JSLevel): GLevel = level match
    case JSLevel.Error => GLevel.ERROR
    case JSLevel.Warn  => GLevel.WARN
    case JSLevel.Info  => GLevel.INFO
    case JSLevel.Debug => GLevel.DEBUG
