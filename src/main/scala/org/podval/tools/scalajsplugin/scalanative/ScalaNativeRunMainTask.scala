package org.podval.tools.scalajsplugin.scalanative

import org.gradle.api.tasks.TaskAction
import org.podval.tools.scalajsplugin.nonjvm.BackendRunMainTask
import scala.sys.process.Process
  
abstract class ScalaNativeRunMainTask extends BackendRunMainTask[ScalaNativeLinkTask] with ScalaNativeRunTask:
  final override protected def linkTaskClass: Class[ScalaNativeLinkMainTask] = classOf[ScalaNativeLinkMainTask]

  @TaskAction final def execute(): Unit = Process(Seq(linkTask.getOutputFile.getAbsolutePath)).!
