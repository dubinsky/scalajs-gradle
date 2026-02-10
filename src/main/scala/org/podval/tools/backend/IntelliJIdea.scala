package org.podval.tools.backend

object IntelliJIdea:
  def runningIn: Boolean = Option(System.getProperty("idea.active")).contains("true")
