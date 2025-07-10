package org.podval.tools.platform

import org.gradle.api.Task
import org.gradle.process.ExecOperations
import javax.inject.Inject

trait TaskWithRunner extends Task:
  @Inject def getExecOperations: ExecOperations

  final protected def runner: Runner = Runner(getExecOperations, getLogger)
