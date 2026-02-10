package org.podval.tools.nonjvm

import org.podval.tools.build.{Runner, TestEnvironment}

trait Run[B <: NonJvmBackend] extends TestEnvironment.Creator[B]:
  def run(runner: Runner): Unit
