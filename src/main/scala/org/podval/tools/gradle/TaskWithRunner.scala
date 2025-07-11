package org.podval.tools.gradle

import org.gradle.api.Task
import org.gradle.process.ExecOperations
import org.podval.tools.platform.Runner
import javax.inject.Inject

trait TaskWithRunner extends Task:
  @Inject def getExecOperations: ExecOperations
  final protected def runner: Runner = Runner(getExecOperations)
