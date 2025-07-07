package org.podval.tools.scalajs

import org.podval.tools.nonjvm.Build
import org.scalajs.logging.{Level as LevelJS, Logger as LoggerJS}
import org.slf4j.event.Level

class ScalaJSBuild(logSource: String) extends Build[LoggerJS](logSource):
  final override protected def interceptedExceptions: Set[Class[? <: Exception]] = Set.empty

  final override protected def backendLogger: LoggerJS = new LoggerJS:
    override def trace(t: => Throwable): Unit = logThrowable(t)

    given CanEqual[LevelJS, LevelJS] = CanEqual.derived
    override def log(level: LevelJS, message: => String): Unit = logAtLevel(
      message,
      level match
        case LevelJS.Error => Level.ERROR
        case LevelJS.Warn  => Level.WARN
        case LevelJS.Info  => Level.INFO
        case LevelJS.Debug => Level.DEBUG
    )
