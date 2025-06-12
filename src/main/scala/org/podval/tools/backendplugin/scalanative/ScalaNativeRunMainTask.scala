package org.podval.tools.backendplugin.scalanative

import org.gradle.api.tasks.TaskAction
import org.gradle.process.{ExecOperations, ExecSpec}
import org.podval.tools.backendplugin.nonjvm.NonJvmRunTask
import org.podval.tools.platform.OutputPiper
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
