package org.podval.tools.nonjvm

import org.gradle.api.GradleException
import org.slf4j.event.Level
import org.slf4j.{Logger, LoggerFactory}

class NonJvmBuild(logSource: String):
  final protected def abort(message: String): Nothing = throw GradleException(s"$logSource: $message")

  final protected val logger: Logger = LoggerFactory.getLogger(getClass)

  final protected def logThrowable(t: Throwable): Unit = logger.error(s"$logSource Error", t)

  final protected def logAtLevel(message: String, level: Level): Unit =
    def toLog(level: Char) = s"$logSource[$level]: $message"

    // Gradle has its own copy of org.slf4j API, which predates introduction of logger.atLevel().
    given CanEqual[Level, Level] = CanEqual.derived
    level match
      case Level.ERROR => logger.error(toLog('e'))
      case Level.WARN  => logger.warn (toLog('w'))
      case Level.INFO  => logger.info (toLog('i'))
      case Level.DEBUG => logger.debug(toLog('d'))
      case Level.TRACE => logger.trace(toLog('t'))
