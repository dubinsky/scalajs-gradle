package org.podval.tools.scalajs

import org.gradle.api.logging.{Logger, LogLevel as GLevel}
import org.scalajs.logging.Level as JSLevel

final class ScalaJSLogger(
  taskName: String,
  logger: Logger
) extends org.scalajs.logging.Logger:
  private def logSource: String = s"ScalaJS $taskName"

  override def trace(t: => Throwable): Unit =
    logger.error(s"$logSource Error", t)

  override def log(level: JSLevel, message: => String): Unit =
    logger.log(ScalaJSLogger.scalajs2gradleLevel(level), s"$logSource: $message")

object ScalaJSLogger:
  private given CanEqual[JSLevel, JSLevel] = CanEqual.derived
  private def scalajs2gradleLevel(level: JSLevel): GLevel = level match
    case JSLevel.Error => GLevel.ERROR
    case JSLevel.Warn  => GLevel.WARN
    case JSLevel.Info  => GLevel.INFO
    case JSLevel.Debug => GLevel.DEBUG
