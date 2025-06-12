package org.podval.tools.backendplugin.nonjvm

import org.podval.tools.backendplugin.BackendTask

trait NonJvmTask[L <: NonJvmLinkTask[L]] extends BackendTask:
  protected def linkTask: L
