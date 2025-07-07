package org.podval.tools.nonjvm

trait Link[B <: NonJvmBackend]:
  def link(): Unit
