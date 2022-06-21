package org.podval.tools.scalajs.testing

import org.gradle.api.logging.Logger
import scala.util.control.NonFatal

final class Listeners(
  listeners: Seq[TestsListener],
  log: Logger
):
  def safeForeach(f: TestsListener => Unit): Unit =
    listeners.foreach((listener: TestsListener) =>
      try f(listener) catch
        case NonFatal(e) =>
          log.trace("", e)
          log.error(e.toString) // TODO the message
    )

  def loggers(name: String): Seq[ContentLogger] =
    listeners.flatMap(_.contentLogger(name))

  def debug(message: String): Unit =
    log.info(message, null, null, null)
