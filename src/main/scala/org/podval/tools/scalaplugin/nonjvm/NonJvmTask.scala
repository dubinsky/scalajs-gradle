package org.podval.tools.scalaplugin.nonjvm

import org.podval.tools.scalaplugin.BackendTask

trait NonJvmTask[L <: NonJvmLinkTask[L]] extends BackendTask:
  protected def linkTask: L
