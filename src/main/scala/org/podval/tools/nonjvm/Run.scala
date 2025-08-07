package org.podval.tools.nonjvm

import org.podval.tools.platform.Runner
import org.podval.tools.test.task.TestEnvironment

trait Run[B <: NonJvmBackend] extends TestEnvironment.Creator[B]:
  def run(runner: Runner): Unit
