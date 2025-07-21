package org.podval.tools.nonjvm

import org.gradle.api.logging.LogLevel
import org.podval.tools.platform.Output

abstract class Build[L](output: Output):
  private def annotation: String = getClass.getName
  final protected def abort(message: String): Nothing = output.abort(message)
  final protected def logThrowable(throwable: Throwable): Unit = output.logThrowable(annotation, throwable)
  final protected def debug(message: String): Unit = output.debug(annotation, message)
  final protected def logAtLevel(logLevel: LogLevel, message: String): Unit = output.logAtLevel(annotation, logLevel, message)

  protected def backendLogger: L

  protected def interceptedExceptions: Set[Class[? <: Exception]]

  final protected def interceptException[T](op: => T): T =
    try op catch
      case e: Exception if interceptedExceptions.exists(_.getName == e.getClass.getName) => abort(e.getMessage)
