package org.podval.tools.nonjvm

import org.podval.tools.build.TestEnvironment
import org.podval.tools.platform.Runner

trait Run[B <: NonJvmBackend] extends TestEnvironment.Creator[B]:
  def run(runner: Runner): Unit
