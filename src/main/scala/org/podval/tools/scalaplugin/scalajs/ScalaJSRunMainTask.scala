package org.podval.tools.scalaplugin.scalajs

import org.gradle.api.tasks.TaskAction
import org.podval.tools.build.scalajs.ScalaJSBuild
import org.podval.tools.scalaplugin.nonjvm.NonJvmRunTask

abstract class ScalaJSRunMainTask extends NonJvmRunTask.Main[ScalaJSLinkTask] with ScalaJSRunTask:
  final override protected def linkTaskClass: Class[ScalaJSLinkMainTask] = classOf[ScalaJSLinkMainTask]

  @TaskAction final def execute(): Unit = ScalaJSBuild.run(
    jsDirectory = linkTask.getJSDirectory,
    reportBinFile = linkTask.getReportBinFile,
    moduleKind = linkTask.moduleKind,
    node = linkTask.node,
    logSource = getName,
    abort = abort,
    outputHandler = outputHandler
  )
