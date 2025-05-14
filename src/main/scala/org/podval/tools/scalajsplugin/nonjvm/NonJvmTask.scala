package org.podval.tools.scalajsplugin.nonjvm

import org.podval.tools.scalajsplugin.BackendTask

trait NonJvmTask[L <: NonJvmLinkTask[L]] extends BackendTask:
  protected def linkTask: L
