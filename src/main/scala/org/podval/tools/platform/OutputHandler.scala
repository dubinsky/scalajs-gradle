package org.podval.tools.platform

import org.gradle.api.logging.Logger

trait OutputHandler:
  def out(message: String): Unit
  def err(message: String): Unit

object OutputHandler:
  def apply(logger: Logger): OutputHandler = new OutputHandler:
    override def out(message: String): Unit = logger.lifecycle(message)
    override def err(message: String): Unit = logger.error(s"err: $message")
