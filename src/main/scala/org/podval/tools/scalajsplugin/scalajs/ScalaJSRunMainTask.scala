package org.podval.tools.scalajsplugin.scalajs

import org.gradle.api.tasks.TaskAction
import org.podval.tools.scalajs.ScalaJSRun
import org.podval.tools.scalajsplugin.nonjvm.NonJvmRunTask

abstract class ScalaJSRunMainTask extends NonJvmRunTask.Main[ScalaJSLinkTask] with ScalaJSRunTask:
  final override protected def linkTaskClass: Class[ScalaJSLinkMainTask] = classOf[ScalaJSLinkMainTask]

  @TaskAction final def execute(): Unit = ScalaJSRun(scalaJSRunCommon).run(outputHandler)
