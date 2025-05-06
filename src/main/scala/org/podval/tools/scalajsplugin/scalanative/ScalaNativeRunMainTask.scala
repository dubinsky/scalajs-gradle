package org.podval.tools.scalajsplugin.scalanative

import org.gradle.api.tasks.TaskAction
import org.podval.tools.scalajsplugin.nonjvm.NonJvmRunMainTask
import scala.sys.process.Process
  
abstract class ScalaNativeRunMainTask extends NonJvmRunMainTask[ScalaNativeLinkTask] with ScalaNativeRunTask:
  final override protected def linkTaskClass: Class[ScalaNativeLinkMainTask] = classOf[ScalaNativeLinkMainTask]

  // TODO if the main class is not set, link with a different build type to avoid errors?
  
  // TODO handle the output the same way it is done for Scala.js
  @TaskAction final def execute(): Unit = Process(Seq(linkTask.getOutputFile.getAbsolutePath)).!
