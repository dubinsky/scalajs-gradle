package org.podval.tools.scalajs.testing

final class SBTTestingLogger(gradleLogger: org.gradle.api.logging.Logger, prefix: String) extends sbt.testing.Logger:
  // TODO make prefix a TestDescriptor and use it to generate events
  private def withPrefix(message: String) =
    // s"$prefix: $message"
    message

  def ansiCodesSupported: Boolean = true

  def error(msg: String): Unit = gradleLogger.error(withPrefix(msg))

  def warn(msg: String): Unit = gradleLogger.warn(withPrefix(msg))

  def info(msg: String): Unit = gradleLogger.lifecycle(withPrefix(msg))

  def debug(msg: String): Unit = gradleLogger.debug(withPrefix(msg), null, null, null)

  def trace(t: Throwable): Unit = gradleLogger.error(withPrefix("Error"), t)
