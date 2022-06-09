package org.podval.tools.scalajs.testing

final class SBTTestingLogger(gradleLogger: org.gradle.api.logging.Logger) extends sbt.testing.Logger:
  def ansiCodesSupported: Boolean = true

  def error(msg: String): Unit = gradleLogger.error(msg)

  def warn(msg: String): Unit = gradleLogger.warn(msg)

  def info(msg: String): Unit = gradleLogger.lifecycle(msg)

  def debug(msg: String): Unit = gradleLogger.debug(msg, null, null, null)

  def trace(t: Throwable): Unit = gradleLogger.error("Error", t)
