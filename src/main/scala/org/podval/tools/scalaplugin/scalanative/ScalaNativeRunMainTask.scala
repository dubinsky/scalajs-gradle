package org.podval.tools.scalaplugin.scalanative

import org.gradle.api.tasks.TaskAction
import org.gradle.process.{ExecOperations, ExecSpec}
import org.podval.tools.platform.OutputPiper
import org.podval.tools.scalaplugin.nonjvm.NonJvmRunTask
import javax.inject.Inject
  
abstract class ScalaNativeRunMainTask  @Inject(execOperations: ExecOperations)
  extends NonJvmRunTask.Main[ScalaNativeLinkTask]
  with ScalaNativeRunTask:
  final override protected def linkTaskClass: Class[ScalaNativeLinkMainTask] = classOf[ScalaNativeLinkMainTask]
  
  @TaskAction final def execute(): Unit =
    val running: String = linkTask.getOutputFile.getAbsolutePath
    execOperations.exec: (exec: ExecSpec) =>
      OutputPiper.run(outputHandler, running): (outputPiper: OutputPiper) =>
        exec.setCommandLine(linkTask.getOutputFile.getAbsolutePath)
        outputPiper.start(exec)
