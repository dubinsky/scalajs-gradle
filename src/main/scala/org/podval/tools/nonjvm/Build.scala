package org.podval.tools.nonjvm

import org.gradle.api.GradleException
import org.slf4j.event.Level
import org.slf4j.{Logger, LoggerFactory}

abstract class Build[L](logSource: String):
  protected def backendLogger: L

  protected def interceptedExceptions: Set[Class[? <: Exception]]

  final protected def abort(message: String): Nothing = throw GradleException(s"$logSource: $message")

  final protected def interceptException[T](op: => T): T =
    try op catch
      case e: Exception if interceptedExceptions.exists(_.getName == e.getClass.getName) => abort(e.getMessage)

  final protected val logger: Logger = LoggerFactory.getLogger(getClass)

  final protected def logThrowable(t: Throwable): Unit = logger.error(s"$logSource Error", t)

  final protected def logAtLevel(message: String, level: Level): Unit =
    def toLog = s"build $logSource $level: $message"

    // Gradle has its own copy of org.slf4j API, which predates introduction of logger.atLevel().
    given CanEqual[Level, Level] = CanEqual.derived
    level match
      case Level.ERROR => logger.error(toLog)
      case Level.WARN  => logger.warn (toLog)
      case Level.INFO  => logger.info (toLog)
      case Level.DEBUG => logger.debug(toLog)
      case Level.TRACE => logger.trace(toLog)
