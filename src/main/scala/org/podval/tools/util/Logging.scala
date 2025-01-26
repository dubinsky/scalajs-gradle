package org.podval.tools.util

object Logging:
  def setInfo(logger: org.slf4j.Logger): Unit = setLevel(logger, ch.qos.logback.classic.Level.INFO)

  def setWarn(logger: org.slf4j.Logger): Unit = setLevel(logger, ch.qos.logback.classic.Level.WARN)

  def setLevel(logger: org.slf4j.Logger, level: ch.qos.logback.classic.Level): Unit = logger match
    case logger: ch.qos.logback.classic.Logger => logger.setLevel(level)
    case _ =>
