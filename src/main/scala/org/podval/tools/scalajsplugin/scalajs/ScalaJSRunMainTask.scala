package org.podval.tools.scalajsplugin.scalajs

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.podval.tools.scalajs.ScalaJSRun

abstract class ScalaJSRunMainTask extends DefaultTask with ScalaJSRunTask:
  setGroup("other")

  final override protected def flavour: String = "Run"

  final override protected def linkTaskClass: Class[ScalaJSLinkMainTask] = classOf[ScalaJSLinkMainTask]

  @TaskAction final def execute(): Unit = ScalaJSRun(scalaJSRunCommon).run()
