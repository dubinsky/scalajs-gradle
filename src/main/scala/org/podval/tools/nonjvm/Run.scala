package org.podval.tools.nonjvm

import org.gradle.process.ExecOperations
import org.podval.tools.build.TestEnvironment
import org.podval.tools.platform.OutputPiper

trait Run[B <: NonJvmBackend] extends TestEnvironment.Creator[B]:
  def run(
    execOperations: ExecOperations,
    outputPiper: OutputPiper
  ): Unit
