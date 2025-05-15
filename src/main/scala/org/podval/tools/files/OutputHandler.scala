package org.podval.tools.files

import org.gradle.api.logging.Logger

trait OutputHandler:
  def out(message: String): Unit
  def err(message: String): Unit

// TODO enhance to handle run sequence for JVM, JS and Native uniformly,
// with logging before and after
// and piping of the streams when needed
object OutputHandler:
  def apply(logger: Logger): OutputHandler = new OutputHandler:
    override def out(message: String): Unit = logger.lifecycle(message)
    override def err(message: String): Unit = logger.error(s"err: $message")
