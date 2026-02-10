package org.podval.tools.build

import org.gradle.process.ExecOperations
import javax.inject.Inject

trait RunnerTask extends OutputTask:
  @Inject def getExecOperations: ExecOperations
  
  final protected def runner: Runner = Runner(
    getExecOperations,
    output
  )
