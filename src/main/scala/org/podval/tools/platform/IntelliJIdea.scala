package org.podval.tools.platform

object IntelliJIdea:
  def runningIn: Boolean = Option(System.getProperty("idea.active")).contains("true")
