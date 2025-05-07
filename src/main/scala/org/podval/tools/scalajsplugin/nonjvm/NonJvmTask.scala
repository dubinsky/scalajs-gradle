package org.podval.tools.scalajsplugin.nonjvm

import org.gradle.api.Task

trait NonJvmTask[L <: NonJvmLinkTask[L]] extends Task:
  protected def linkTask: L
