package org.podval.tools.platform

import org.gradle.api.GradleException
import org.gradle.api.logging.{Logger, Logging, LogLevel}

final class Output(
  val logLevelEnabled: LogLevel,
  val isRunningInIntelliJ: Boolean,
  val logSource: String
) extends Serializable:
  def abort(message: String): Nothing = throw GradleException(annotate("", message))

  // Output gets serialized, but the Logger is not serializable, so - lazy:
  private lazy val logger: Logger = Logging.getLogger(getClass)

  def debug    (annotation: String, message: String): Unit = logAtLevel(annotation, LogLevel.DEBUG    , message)
  def info     (annotation: String, message: String): Unit = logAtLevel(annotation, LogLevel.INFO     , message)
  def lifecycle(annotation: String, message: String): Unit = logAtLevel(annotation, LogLevel.LIFECYCLE, message)
  def warn     (annotation: String, message: String): Unit = logAtLevel(annotation, LogLevel.WARN     , message)

  def logAtLevel(annotation: String, logLevel: LogLevel, message: String): Unit =
    logger.log(logLevel, annotate(annotation, logLevel, message))

  def logThrowable(annotation: String, throwable: Throwable): Unit =
    logger.error(annotate(annotation, "throwable"), throwable)

  def isVisible(logLevel: LogLevel): Boolean =
    Output.isVisible(logLevel, logLevelEnabled) ||
    Output.isVisible(logLevel, LogLevel.INFO) && isRunningInIntelliJ

  def annotate(annotation: String, logLevel: LogLevel, message: String): String =
    annotate(s"$annotation $logLevel", message)

  private def annotate(annotation: String, message: String): String =
    if !Output.annotate
    then message
    else s"[$annotation $logSource]: $message"

object Output:
  private val annotate: Boolean = false

  given CanEqual[LogLevel, LogLevel] = CanEqual.derived

  def isError(logLevel: LogLevel): Boolean =
    (logLevel == LogLevel.ERROR) ||
    (logLevel == LogLevel.WARN)

  private def isVisible(logLevel: LogLevel, logLevelEnabled: LogLevel): Boolean =
    Output.priority(logLevel) >= Output.priority(logLevelEnabled)

  private val logLevelsByPriority: List[LogLevel] = List(
    LogLevel.DEBUG,
    LogLevel.INFO,
    LogLevel.LIFECYCLE,
    LogLevel.WARN,
    LogLevel.QUIET,
    LogLevel.ERROR
  )

  private def priority(logLevel: LogLevel): Int =
    logLevelsByPriority.indexOf(logLevel)
    // logLevel.ordinal
