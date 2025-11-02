package org.podval.tools.scalajs

import org.gradle.api.logging.LogLevel
import org.podval.tools.nonjvm.Build
import org.podval.tools.platform.Output
import org.scalajs.logging.{Level as LevelJS, Logger as LoggerJS}

open class ScalaJSBuild(output: Output) extends Build[LoggerJS](output):
  final override protected def interceptedExceptions: Set[Class[? <: Exception]] = Set.empty

  final override protected def backendLogger: LoggerJS = new LoggerJS:
    override def trace(throwable: => Throwable): Unit = logThrowable(throwable)

    given CanEqual[LevelJS, LevelJS] = CanEqual.derived
    override def log(logLevel: LevelJS, message: => String): Unit = logAtLevel(
      message = message,
      logLevel = logLevel match
        case LevelJS.Error => LogLevel.ERROR
        case LevelJS.Warn  => LogLevel.WARN
        case LevelJS.Info  => LogLevel.INFO
        case LevelJS.Debug => LogLevel.DEBUG
    )
