package org.podval.tools.scalajsplugin.scalanative

import org.gradle.api.tasks.TaskAction
import org.podval.tools.platform.Exec
import org.podval.tools.scalajsplugin.nonjvm.NonJvmRunMainTask
  
// TODO add properties to set arguments
abstract class ScalaNativeRunMainTask extends NonJvmRunMainTask[ScalaNativeLinkTask] with ScalaNativeRunTask:
  final override protected def linkTaskClass: Class[ScalaNativeLinkMainTask] = classOf[ScalaNativeLinkMainTask]
  
  @TaskAction final def execute(): Unit = Exec(Seq(linkTask.getOutputFile.getAbsolutePath))
